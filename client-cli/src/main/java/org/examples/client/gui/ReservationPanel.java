package org.examples.client.gui;

import org.examples.client.MiniJson;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Panneau de confirmation et finalisation de réservation
 */
public class ReservationPanel extends JPanel {

    private HotelClientGUI mainFrame;
    private JTextArea offerDetailsArea;
    private JTextField lastNameField;
    private JTextField firstNameField;
    private JTextField cardNumberField;
    private JButton confirmButton;
    private JButton cancelButton;
    private JLabel statusLabel;

    private String currentOffer;
    private Date startDate;
    private Date endDate;

    public ReservationPanel(HotelClientGUI mainFrame) {
        this.mainFrame = mainFrame;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 245, 250));

        // Titre
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        JLabel titleLabel = new JLabel("✅ Finaliser la Réservation");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.BLACK);
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // Panneau principal
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(20, 20, 20, 20)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Détails de l'offre
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.4;

        JPanel offerPanel = new JPanel(new BorderLayout());
        offerPanel.setBorder(new TitledBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2),
            "📋 Détails de votre réservation",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            Color.BLACK
        ));
        offerPanel.setBackground(Color.WHITE);

        offerDetailsArea = new JTextArea();
        offerDetailsArea.setEditable(false);
        offerDetailsArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        offerDetailsArea.setBackground(new Color(250, 250, 250));
        offerDetailsArea.setForeground(Color.BLACK);
        offerDetailsArea.setLineWrap(true);
        offerDetailsArea.setWrapStyleWord(true);

        JScrollPane offerScroll = new JScrollPane(offerDetailsArea);
        offerScroll.setBorder(BorderFactory.createEmptyBorder());
        offerPanel.add(offerScroll, BorderLayout.CENTER);

        mainPanel.add(offerPanel, gbc);

        // Formulaire client
        gbc.gridy = 1;
        gbc.weighty = 0.4;

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new TitledBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2),
            "👤 Vos informations",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            Color.BLACK
        ));
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.fill = GridBagConstraints.HORIZONTAL;
        formGbc.insets = new Insets(8, 10, 8, 10);

        // Nom
        formGbc.gridx = 0;
        formGbc.gridy = 0;
        formGbc.weightx = 0.3;
        JLabel lastNameLabel = new JLabel("Nom:");
        lastNameLabel.setFont(new Font("Arial", Font.BOLD, 13));
        lastNameLabel.setForeground(Color.BLACK);
        formPanel.add(lastNameLabel, formGbc);

        formGbc.gridx = 1;
        formGbc.weightx = 0.7;
        lastNameField = new JTextField(20);
        lastNameField.setFont(new Font("Arial", Font.PLAIN, 13));
        lastNameField.setPreferredSize(new Dimension(250, 30));
        formPanel.add(lastNameField, formGbc);

        // Prénom
        formGbc.gridx = 0;
        formGbc.gridy = 1;
        formGbc.weightx = 0.3;
        JLabel firstNameLabel = new JLabel("Prénom:");
        firstNameLabel.setFont(new Font("Arial", Font.BOLD, 13));
        firstNameLabel.setForeground(Color.BLACK);
        formPanel.add(firstNameLabel, formGbc);

        formGbc.gridx = 1;
        formGbc.weightx = 0.7;
        firstNameField = new JTextField(20);
        firstNameField.setFont(new Font("Arial", Font.PLAIN, 13));
        firstNameField.setPreferredSize(new Dimension(250, 30));
        formPanel.add(firstNameField, formGbc);

        // Numéro de carte
        formGbc.gridx = 0;
        formGbc.gridy = 2;
        formGbc.weightx = 0.3;
        JLabel cardLabel = new JLabel("Carte bancaire:");
        cardLabel.setFont(new Font("Arial", Font.BOLD, 13));
        cardLabel.setForeground(Color.BLACK);
        formPanel.add(cardLabel, formGbc);

        formGbc.gridx = 1;
        formGbc.weightx = 0.7;
        cardNumberField = new JTextField(20);
        cardNumberField.setFont(new Font("Arial", Font.PLAIN, 13));
        cardNumberField.setPreferredSize(new Dimension(250, 30));
        formPanel.add(cardNumberField, formGbc);

        mainPanel.add(formPanel, gbc);

        // Boutons
        gbc.gridy = 2;
        gbc.weighty = 0.2;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        buttonPanel.setOpaque(false);

        cancelButton = new JButton("❌ Annuler");
        cancelButton.setFont(new Font("Arial", Font.PLAIN, 14));
        cancelButton.setPreferredSize(new Dimension(150, 45));
        cancelButton.setBackground(Color.LIGHT_GRAY);
        cancelButton.setForeground(Color.BLACK);
        cancelButton.setFocusPainted(false);
        cancelButton.setBorderPainted(false);
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(e -> mainFrame.showPanel("RESULTS"));

        confirmButton = new JButton("✅ Confirmer la Réservation");
        confirmButton.setFont(new Font("Arial", Font.BOLD, 16));
        confirmButton.setPreferredSize(new Dimension(280, 45));
        confirmButton.setBackground(Color.LIGHT_GRAY);
        confirmButton.setForeground(Color.BLACK);
        confirmButton.setFocusPainted(false);
        confirmButton.setBorderPainted(false);
        confirmButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        confirmButton.addActionListener(e -> makeReservation());

        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Statut
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void setReservationData(String offer, Date start, Date end) {
        this.currentOffer = offer;
        this.startDate = start;
        this.endDate = end;

        // Vider les champs
        lastNameField.setText("");
        firstNameField.setText("");
        cardNumberField.setText("");
        statusLabel.setText(" ");

        // Afficher les détails
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String hotelName = MiniJson.getString(offer, "hotelName");
            String city = MiniJson.getString(offer, "ville");
            String category = MiniJson.getString(offer, "categorie");
            // Extraire le numéro de chambre depuis l'objet "room"
            String roomObj = MiniJson.getObject(offer, "room");
            Integer roomNumInt = roomObj != null ? MiniJson.getInt(roomObj, "numero") : null;
            String roomNum = roomNumInt != null ? String.valueOf(roomNumInt) : "?";
            Integer bedsInt = roomObj != null ? MiniJson.getInt(roomObj, "nbLits") : null;
            String beds = bedsInt != null ? String.valueOf(bedsInt) : "?";
            Integer priceInt = MiniJson.getInt(offer, "prixTotal");
            String price = priceInt != null ? String.valueOf(priceInt) : "0";
            String reference = MiniJson.getString(offer, "offerId");

            // Calculer le nombre de nuits
            long diffInMillies = Math.abs(end.getTime() - start.getTime());
            long nights = diffInMillies / (1000 * 60 * 60 * 24);

            double totalPrice = 0;
            try {
                totalPrice = Double.parseDouble(price) * nights;
            } catch (Exception e) {
                // Ignorer
            }

            StringBuilder details = new StringBuilder();
            details.append("════════════════════════════════════════════════════\n");
            details.append("            RÉSUMÉ DE VOTRE RÉSERVATION\n");
            details.append("════════════════════════════════════════════════════\n\n");
            details.append("🏨 Hôtel:          ").append(hotelName).append("\n");
            details.append("📍 Ville:          ").append(city).append("\n");
            details.append("⭐ Catégorie:      ").append(category).append("\n");
            details.append("🚪 Chambre:        N°").append(roomNum).append("\n");
            details.append("🛏️  Lits:          ").append(beds).append("\n\n");
            details.append("📅 Arrivée:        ").append(sdf.format(start)).append("\n");
            details.append("📅 Départ:         ").append(sdf.format(end)).append("\n");
            details.append("🌙 Nombre de nuits: ").append(nights).append("\n\n");
            details.append("💰 Prix par nuit:  ").append(price).append(" €\n");
            details.append("💳 TOTAL:          ").append(String.format("%.2f", totalPrice)).append(" €\n\n");
            details.append("📝 Référence:      ").append(reference).append("\n");
            details.append("════════════════════════════════════════════════════");

            offerDetailsArea.setText(details.toString());
            offerDetailsArea.setCaretPosition(0);

        } catch (Exception e) {
            offerDetailsArea.setText("Erreur lors de l'affichage des détails.");
            e.printStackTrace();
        }
    }

    private void makeReservation() {
        // Validation
        String lastName = lastNameField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String cardNumber = cardNumberField.getText().trim();

        if (lastName.isEmpty() || firstName.isEmpty() || cardNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Veuillez remplir tous les champs",
                "Champs manquants",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        statusLabel.setText("Réservation en cours...");
        statusLabel.setForeground(Color.BLACK);
        confirmButton.setEnabled(false);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String hotelCode = MiniJson.getString(currentOffer, "hotelCode");
                String offerId = MiniJson.getString(currentOffer, "offerId");
                if (hotelCode == null) hotelCode = MiniJson.getString(currentOffer, "reference");
                if (offerId == null) offerId = MiniJson.getString(currentOffer, "reference");

                // Utilisation de la méthode reserve de AgencyTcpClient
                return mainFrame.getAgencyClient().reserve(
                    hotelCode,
                    offerId,
                    null, // agencyId
                    lastName,
                    firstName,
                    cardNumber
                );
            }

            @Override
            protected void done() {
                confirmButton.setEnabled(true);
                try {
                    String result = get();
                    // L'agence retourne "success" (booléen) et non "status" (string)
                    Boolean successBool = MiniJson.getBoolean(result, "success");
                    boolean success = successBool != null && successBool;
                    String message = MiniJson.getString(result, "message");
                    String reservationRef = MiniJson.getString(result, "reference");

                    if (success) {
                        statusLabel.setText("✅ Réservation confirmée !");
                        statusLabel.setForeground(Color.BLACK);

                        JOptionPane.showMessageDialog(ReservationPanel.this,
                            "Réservation confirmée avec succès !\n\n" +
                            "Référence: " + (reservationRef != null ? reservationRef : "N/A") + "\n" +
                            (message != null ? message : ""),
                            "Succès",
                            JOptionPane.INFORMATION_MESSAGE);

                        // Retour à l'accueil
                        mainFrame.showPanel("WELCOME");

                    } else {
                        statusLabel.setText("❌ Échec de la réservation");
                        statusLabel.setForeground(Color.BLACK);

                        JOptionPane.showMessageDialog(ReservationPanel.this,
                            "La réservation a échoué:\n" + (message != null ? message : "Erreur inconnue"),
                            "Échec",
                            JOptionPane.ERROR_MESSAGE);
                    }

                } catch (Exception ex) {
                    statusLabel.setText("❌ Erreur");
                    statusLabel.setForeground(Color.BLACK);

                    JOptionPane.showMessageDialog(ReservationPanel.this,
                        "Erreur lors de la réservation:\n" + ex.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}

