package org.examples.server.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chambres")
public class ChambreEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false)
    private int nbLits;

    @Column(nullable = false)
    private int prixParNuit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private HotelEntity hotel;

    @OneToMany(mappedBy = "chambre", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationEntity> reservations = new ArrayList<>();

    public ChambreEntity() {}

    public ChambreEntity(int numero, int nbLits, int prixParNuit) {
        this.numero = numero;
        this.nbLits = nbLits;
        this.prixParNuit = prixParNuit;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public int getNbLits() { return nbLits; }
    public void setNbLits(int nbLits) { this.nbLits = nbLits; }

    public int getPrixParNuit() { return prixParNuit; }
    public void setPrixParNuit(int prixParNuit) { this.prixParNuit = prixParNuit; }

    public HotelEntity getHotel() { return hotel; }
    public void setHotel(HotelEntity hotel) { this.hotel = hotel; }

    public List<ReservationEntity> getReservations() { return reservations; }
    public void setReservations(List<ReservationEntity> reservations) { this.reservations = reservations; }

    public void addReservation(ReservationEntity reservation) {
        reservations.add(reservation);
        reservation.setChambre(this);
    }
}

