package Impl;

import Impl.Client;
import Impl.Hotel;
import Impl.Reservation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Chambre {
    private final Hotel hotel;
    private int nbLits, prixParNuit, numero;
    private final List<Reservation> reservations = new ArrayList<>();

    public Chambre(Hotel hotel, int numero, int nbLits, int prixParNuit) {
        this.hotel = hotel; this.numero = numero; this.nbLits = nbLits; this.prixParNuit = prixParNuit;
    }
    public int getNbLits() { return nbLits; }
    public int getPrixParNuit() { return prixParNuit; }
    public int getNumero() { return numero; }
    public Hotel getHotel() { return hotel; }
    public void setPrixParNuit(int prix) { this.prixParNuit = prix; }
    public void setNumero(int num) { this.numero = num; }
    public List<Reservation> getReservations() { return reservations; }

    public boolean isDisponible(LocalDate debut, LocalDate fin) {
        for (Reservation r : reservations) {
            if (Reservation.chevauche(debut, fin, r.getDebut(), r.getFin())) return false;
        }
        return true;
    }

    public int prixTotal(LocalDate debut, LocalDate fin) {
        long nuits = ChronoUnit.DAYS.between(debut, fin);
        if (nuits < 0) throw new IllegalArgumentException("Dates invalides");
        return Math.toIntExact(nuits) * prixParNuit;
    }

    public Reservation reserver(Client c, LocalDate debut, LocalDate fin) {
        if (!isDisponible(debut, fin)) throw new IllegalStateException("Chambre déjà réservée sur la période");
        Reservation res = new Reservation(this, c, debut, fin);
        reservations.add(res);
        return res;
    }
}
