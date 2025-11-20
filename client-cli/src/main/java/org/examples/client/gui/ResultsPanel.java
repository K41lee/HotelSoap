package org.examples.client.gui;

import org.examples.client.MiniJson;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Panneau d'affichage des résultats de recherche
 */
public class ResultsPanel extends JPanel {

    private HotelClientGUI mainFrame;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JButton reserveButton;
    private JButton backButton;
    private JLabel infoLabel;

    private String searchCity;
    private Date searchStart;
    private Date searchEnd;
    private int searchBeds;
    private String currentOffersJson;
    private List<String> imageUrls = new ArrayList<>();

    public ResultsPanel(HotelClientGUI mainFrame) {
        this.mainFrame = mainFrame;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 245, 250));

        // Titre et info
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("📋 Résultats de Recherche");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.BLACK);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        infoLabel = new JLabel(" ");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        infoLabel.setForeground(Color.BLACK);
        infoLabel.setBorder(new EmptyBorder(5, 0, 10, 0));
        topPanel.add(infoLabel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // Table des résultats
        String[] columns = {"Hôtel", "Ville", "Catégorie", "Chambre", "Lits", "Prix/Nuit", "Référence", "Image"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.setRowHeight(35);
        resultsTable.setFont(new Font("Arial", Font.PLAIN, 13));
        resultsTable.setForeground(Color.BLACK);
        // Forcer la couleur de texte de sélection en noir
        resultsTable.setSelectionForeground(Color.BLACK);
        resultsTable.setSelectionBackground(Color.LIGHT_GRAY);
        resultsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        resultsTable.getTableHeader().setBackground(Color.LIGHT_GRAY);
        resultsTable.getTableHeader().setForeground(Color.BLACK);
        resultsTable.setGridColor(new Color(200, 200, 200));

        // Alternance de couleurs des lignes avec texte NOIR forcé
        resultsTable.setDefaultRenderer(Object.class, new TableCellRenderer() {
            private final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Forcer la couleur du texte en NOIR pour toutes les cellules
                if (c instanceof JLabel) {
                    ((JLabel) c).setForeground(Color.BLACK);
                }
                c.setForeground(Color.BLACK);

                if (isSelected) {
                    c.setBackground(Color.LIGHT_GRAY);
                    // Forcer le texte en noir même en sélection
                    if (c instanceof JLabel) {
                        ((JLabel) c).setForeground(Color.BLACK);
                    }
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
                }

                return c;
            }
        });

        // Gestionnaire de clic pour la colonne Image
        resultsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = resultsTable.rowAtPoint(e.getPoint());
                int col = resultsTable.columnAtPoint(e.getPoint());

                // Colonne 7 = colonne Image
                if (row >= 0 && col == 7 && row < imageUrls.size()) {
                    String imageUrl = imageUrls.get(row);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        showImageDialog(imageUrl, row);
                    } else {
                        JOptionPane.showMessageDialog(ResultsPanel.this,
                            "Aucune image disponible pour cette chambre.",
                            "Image non disponible",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });

        // Changer le curseur au survol de la colonne Image
        resultsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = resultsTable.rowAtPoint(e.getPoint());
                int col = resultsTable.columnAtPoint(e.getPoint());

                if (col == 7 && row >= 0 && row < imageUrls.size() &&
                    imageUrls.get(row) != null && !imageUrls.get(row).isEmpty()) {
                    resultsTable.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    resultsTable.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        add(scrollPane, BorderLayout.CENTER);

        // Panneau de boutons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        bottomPanel.setOpaque(false);

        backButton = new JButton("⬅️ Nouvelle Recherche");
        backButton.setFont(new Font("Arial", Font.PLAIN, 14));
        backButton.setPreferredSize(new Dimension(200, 45));
        backButton.setBackground(Color.LIGHT_GRAY);
        backButton.setForeground(Color.BLACK);
        backButton.setFocusPainted(false);
        backButton.setBorderPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> mainFrame.showPanel("SEARCH"));

        reserveButton = new JButton("✅ Réserver la Chambre Sélectionnée");
        reserveButton.setFont(new Font("Arial", Font.BOLD, 16));
        reserveButton.setPreferredSize(new Dimension(320, 45));
        reserveButton.setBackground(Color.LIGHT_GRAY);
        reserveButton.setForeground(Color.BLACK);
        reserveButton.setFocusPainted(false);
        reserveButton.setBorderPainted(false);
        reserveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        reserveButton.setEnabled(false);
        reserveButton.addActionListener(e -> proceedToReservation());

        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            reserveButton.setEnabled(resultsTable.getSelectedRow() >= 0);
        });

        bottomPanel.add(backButton);
        bottomPanel.add(reserveButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void displayResults(String offersJson, String city, Date start, Date end, int beds) {
        this.currentOffersJson = offersJson;
        this.searchCity = city;
        this.searchStart = start;
        this.searchEnd = end;
        this.searchBeds = beds;

        // Mise à jour info
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        infoLabel.setText(String.format("📍 %s | 📅 %s → %s | 🛏️ %d lit(s)",
            city, sdf.format(start), sdf.format(end), beds));

        // Vider la table et la liste d'images
        tableModel.setRowCount(0);
        imageUrls.clear();

        try {
            List<String> offers = MiniJson.getStringArray(offersJson, "offers");

            if (offers == null || offers.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Aucune offre trouvée pour vos critères.",
                    "Aucun résultat",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            for (String offer : offers) {
                String hotelName = MiniJson.getString(offer, "hotelName");
                String hotelCity = MiniJson.getString(offer, "ville");
                String category = MiniJson.getString(offer, "categorie");
                // Extraire les données de la chambre depuis l'objet "room"
                String roomObj = MiniJson.getObject(offer, "room");
                Integer roomNumInt = roomObj != null ? MiniJson.getInt(roomObj, "numero") : null;
                String roomNum = roomNumInt != null ? String.valueOf(roomNumInt) : null;
                Integer bedsInt = roomObj != null ? MiniJson.getInt(roomObj, "nbLits") : null;
                String bedsStr = bedsInt != null ? String.valueOf(bedsInt) : null;
                Integer priceInt = MiniJson.getInt(offer, "prixTotal");
                String priceStr = priceInt != null ? String.valueOf(priceInt) : null;
                String reference = MiniJson.getString(offer, "offerId");
                String imageUrl = MiniJson.getString(offer, "imageUrl");

                // Stocker l'URL de l'image
                imageUrls.add(imageUrl != null ? imageUrl : "");

                tableModel.addRow(new Object[]{
                    hotelName != null ? hotelName : "?",
                    hotelCity != null ? hotelCity : "?",
                    category != null ? category : "?",
                    roomNum != null ? "N°" + roomNum : "?",
                    bedsStr != null ? bedsStr : "?",
                    priceStr != null ? priceStr + " €" : "?",
                    reference != null ? reference : "?",
                    imageUrl != null && !imageUrl.isEmpty() ? "🖼️ Voir" : "-"
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Erreur lors de l'affichage des résultats:\n" + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void proceedToReservation() {
        int selectedRow = resultsTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        try {
            List<String> offers = MiniJson.getStringArray(currentOffersJson, "offers");
            if (offers != null && selectedRow < offers.size()) {
                String selectedOffer = offers.get(selectedRow);

                // Passer au panneau de réservation
                ReservationPanel reservationPanel = (ReservationPanel) ((JPanel) mainFrame.getContentPane().getComponent(0))
                    .getComponent(3);
                reservationPanel.setReservationData(selectedOffer, searchStart, searchEnd);
                mainFrame.showPanel("RESERVATION");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Erreur lors de la sélection:\n" + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Affiche l'image de la chambre dans une nouvelle fenêtre
     */
    private void showImageDialog(String imageUrl, int row) {
        JDialog imageDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "🖼️ Image de la Chambre", true);
        imageDialog.setLayout(new BorderLayout());
        imageDialog.setSize(600, 500);
        imageDialog.setLocationRelativeTo(this);

        try {
            // Décoder l'image depuis le data URL
            if (imageUrl.startsWith("data:image")) {
                // Extraire le Base64
                String base64Data = imageUrl.substring(imageUrl.indexOf(",") + 1);
                byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);

                // Créer l'image
                ImageIcon imageIcon = new ImageIcon(imageBytes);

                // Redimensionner si nécessaire
                Image image = imageIcon.getImage();
                Image scaledImage = image.getScaledInstance(550, 400, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);

                JLabel imageLabel = new JLabel(scaledIcon);
                imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

                JScrollPane scrollPane = new JScrollPane(imageLabel);
                imageDialog.add(scrollPane, BorderLayout.CENTER);
            } else {
                // Si ce n'est pas un data URL, afficher un message
                JLabel messageLabel = new JLabel("Format d'image non supporté", SwingConstants.CENTER);
                messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                imageDialog.add(messageLabel, BorderLayout.CENTER);
            }

            // Bouton de fermeture
            JPanel buttonPanel = new JPanel();
            JButton closeButton = new JButton("Fermer");
            closeButton.setPreferredSize(new Dimension(100, 35));
            closeButton.setBackground(Color.LIGHT_GRAY);
            closeButton.setForeground(Color.BLACK);
            closeButton.addActionListener(e -> imageDialog.dispose());
            buttonPanel.add(closeButton);
            imageDialog.add(buttonPanel, BorderLayout.SOUTH);

            imageDialog.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Erreur lors du chargement de l'image:\n" + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}

