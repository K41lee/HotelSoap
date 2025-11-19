package org.examples.client.gui;

import org.examples.client.MiniJson;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.text.SimpleDateFormat;
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
        String[] columns = {"Hôtel", "Ville", "Catégorie", "Chambre", "Lits", "Prix/Nuit", "Référence"};
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

        // Vider la table
        tableModel.setRowCount(0);

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

                tableModel.addRow(new Object[]{
                    hotelName != null ? hotelName : "?",
                    hotelCity != null ? hotelCity : "?",
                    category != null ? category : "?",
                    roomNum != null ? "N°" + roomNum : "?",
                    bedsStr != null ? bedsStr : "?",
                    priceStr != null ? priceStr + " €" : "?",
                    reference != null ? reference : "?"
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
}

