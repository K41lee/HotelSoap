package org.examples.agency;

import org.examples.server.soap.HotelService;
import org.examples.server.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

@Service
public class AgencyService {
  private static final Logger log = LoggerFactory.getLogger(AgencyService.class);

  private static class Partner {
    final String code; final String wsdl; final String serviceNs; final String portNs; final String serviceName; final String portName;
    Partner(String code, String wsdl, String serviceNs, String portNs, String serviceName, String portName){
      this.code=code; this.wsdl=wsdl; this.serviceNs=serviceNs; this.portNs=portNs; this.serviceName=serviceName; this.portName=portName;
    }
  }
  private final List<Partner> partners = Arrays.asList(
      new Partner("rivage", "http://localhost:8081/hotel-rivage/hotel?wsdl", "http://soap.server.examples.org/", "http://soap.server.examples.org/", "HotelService", "HotelServiceImplPort"),
      new Partner("opera",  "http://localhost:8082/hotel-opera/hotel?wsdl",   "http://soap.server.examples.org/", "http://soap.server.examples.org/", "HotelService", "HotelServiceImplPort")
  );

  public String handleRequest(String jsonLine) {
    try {
      log.info("[AGENCY-REQ] raw={}", jsonLine);
      Map<String,Object> req = Json.minParse(jsonLine);
      String op = (String) req.get("op");
      if ("ping".equals(op)) return Json.ok(Collections.singletonMap("pong", true));
      if ("catalog.get".equals(op)) { log.info("[AGENCY] op=catalog.get"); return Json.ok(getCatalog()); }
      if ("offers.search".equals(op)) { log.info("[AGENCY] op=offers.search payload={}", req.get("payload")); return Json.ok(searchOffers((Map<String,Object>) req.get("payload"))); }
      if ("reservation.make".equals(op)) { Map<String,Object> p=(Map<String,Object>)req.get("payload");
        String masked = p!=null && p.get("carte")!=null? maskCard(String.valueOf(p.get("carte"))) : null;
        log.info("[AGENCY] op=reservation.make payload={{hotelCode={}, offerId={}, agencyId={}, nom={}, prenom={}, carte={}}}",
                 p!=null? p.get("hotelCode"):null, p!=null? p.get("offerId"):null, p!=null? p.get("agencyId"):null,
                 p!=null? p.get("nom"):null, p!=null? p.get("prenom"):null, masked);
        return Json.ok(makeReservation(p)); }
      return Json.error("unknown op");
    } catch (Exception e) {
      log.warn("[AGENCY] handle error: {}", e.toString());
      return Json.error(e.getMessage());
    }
  }

  private Map<String,Object> getCatalog() {
    Set<String> cities = new LinkedHashSet<>();
    Set<String> agencies = new LinkedHashSet<>();
    for (Partner p : partners) {
      try {
        HotelService port = port(p);
        Catalog cat = port.getCatalog();
        if (cat!=null) {
          if (cat.getCities()!=null) cities.addAll(cat.getCities().getCity());
          if (cat.getAgencies()!=null) agencies.addAll(cat.getAgencies().getAgency());
        }
      } catch (Exception e) { log.warn("[AGENCY-CALL] catalog {} failed: {}", p.code, e.toString()); }
    }
    Map<String,Object> data = new LinkedHashMap<>();
    data.put("name", "Agence Centrale");
    data.put("cities", new ArrayList<>(cities));
    data.put("agencies", new ArrayList<>(agencies));
    return data;
  }

  private Map<String,Object> searchOffers(Map<String,Object> payload) throws Exception {
    String ville = str(payload.get("ville"));
    String arrivee = str(payload.get("arrivee"));
    String depart = str(payload.get("depart"));
    int nb = num(payload.get("nbPersonnes"), 1);
    String agencyId = str(payload.get("agencyId"));
    log.info("[AGENCY] searchOffers ville='{}' arrivee='{}' depart='{}' nbPersonnes={} agencyId='{}'", ville, arrivee, depart, nb, agencyId);

    LocalDate from = LocalDate.parse(arrivee); LocalDate to = LocalDate.parse(depart);
    javax.xml.datatype.DatatypeFactory df = javax.xml.datatype.DatatypeFactory.newInstance();

    List<Map<String,Object>> offers = new ArrayList<>();
    for (Partner p : partners) {
      try {
        HotelService port = port(p);
        SearchCriteria sc = new SearchCriteria();
        sc.setVille(ville);
        sc.setArrivee(df.newXMLGregorianCalendarDate(from.getYear(), from.getMonthValue(), from.getDayOfMonth(), javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED));
        sc.setDepart(df.newXMLGregorianCalendarDate(to.getYear(), to.getMonthValue(), to.getDayOfMonth(), javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED));
        sc.setNbPersonnes(nb);
        String effectiveAgency = agencyId;
        if (effectiveAgency == null || effectiveAgency.trim().isEmpty()) {
          if ("rivage".equals(p.code)) effectiveAgency = "rivageAgency";
          else if ("opera".equals(p.code)) effectiveAgency = "operaAgency";
        }
        sc.setAgence(effectiveAgency);
        log.info("[AGENCY->HOTEL:{}] searchOffers agence='{}' ville='{}' from='{}' to='{}' nb={}", p.code, effectiveAgency, ville, from, to, nb);
        SearchOffersResponse resp = port.searchOffers(sc);
        int count = (resp!=null && resp.getOffers()!=null && resp.getOffers().getOffers()!=null)? resp.getOffers().getOffers().size() : 0;
        log.info("[HOTEL:{}->AGENCY] offers count={}", p.code, count);
        if (resp!=null && resp.getOffers()!=null && resp.getOffers().getOffers()!=null) {
          for (Offer o : resp.getOffers().getOffers()) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("hotelCode", p.code);
            m.put("hotelName", o.hotelName);
            m.put("categorie", o.categorie);
            m.put("nbEtoiles", o.nbEtoiles);
            if (o.address!=null) {
              Map<String,Object> a = new LinkedHashMap<>();
              a.put("pays", o.address.pays);
              a.put("ville", o.address.ville);
              a.put("rue", o.address.rue);
              a.put("numero", o.address.numero);
              m.put("address", a);
              m.put("city", o.address.ville);
              // aplatir pour le client existant
              m.put("pays", o.address.pays);
              m.put("ville", o.address.ville);
              m.put("rue", o.address.rue);
              m.put("numero", o.address.numero);
            }
            if (o.room!=null) {
              Map<String,Object> r = new LinkedHashMap<>();
              r.put("numero", o.room.numero);
              r.put("nbLits", o.room.nbLits);
              m.put("room", r);
            }
            m.put("start", o.start!=null? o.start.toXMLFormat(): null);
            m.put("end", o.end!=null? o.end.toXMLFormat(): null);
            m.put("prixTotal", o.prixTotal);
            m.put("agenceApplied", o.agenceApplied);
            m.put("offerId", o.offerId);
            offers.add(m);
          }
        }
        log.info("[AGENCY-CALL] offers {} -> {} items", p.code, offers.size());
      } catch (Exception e) {
        log.warn("[AGENCY-CALL] offers {} failed: {}", p.code, e.toString());
      }
    }
    Map<String,Object> data = new LinkedHashMap<>();
    data.put("offers", offers);
    return data;
  }

  private Map<String,Object> makeReservation(Map<String,Object> payload) throws Exception {
    String hotelCode = str(payload.get("hotelCode"));
    String offerId = str(payload.get("offerId"));
    String agencyId = str(payload.get("agencyId"));
    String nom = str(payload.get("nom"));
    String prenom = str(payload.get("prenom"));
    String carte = str(payload.get("carte"));
    log.info("[AGENCY] makeReservation hotelCode='{}' offerId='{}' agencyId='{}' nom='{}' prenom='{}' carte='{}'",
             hotelCode, offerId, agencyId, nom, prenom, maskCard(carte));

    Partner target = partners.stream().filter(p -> p.code.equals(hotelCode)).findFirst().orElse(null);
    if (target == null) throw new IllegalArgumentException("unknown hotelCode");

    HotelService port = port(target);
    ReservationRequest rq = new ReservationRequest();
    rq.offerId = offerId;
    rq.agence = agencyId;
    rq.nom = nom; rq.prenom = prenom; rq.carte = carte;
    log.info("[AGENCY->HOTEL:{}] reserve offerId='{}' agencyId='{}' nom='{}' prenom='{}'", target.code, offerId, agencyId, nom, prenom);
    ReservationConfirmation conf = port.makeReservation(rq);
    log.info("[HOTEL:{}->AGENCY] reserve success={} ref={} message={} ", target.code, conf.isSuccess(), conf.getReference(), conf.getMessage());

    Map<String,Object> data = new LinkedHashMap<>();
    data.put("success", conf.isSuccess());
    data.put("message", conf.getMessage());
    data.put("reference", conf.getReference());
    return data;
  }

  private HotelService port(Partner p) throws Exception {
    URL wsdl = new URL(p.wsdl);
    javax.xml.namespace.QName serviceName = new javax.xml.namespace.QName(p.serviceNs, p.serviceName);
    javax.xml.namespace.QName portName = new javax.xml.namespace.QName(p.portNs, p.portName);
    javax.xml.ws.Service svc = javax.xml.ws.Service.create(wsdl, serviceName);
    HotelService port = svc.getPort(portName, HotelService.class);
    BindingProvider bp = (BindingProvider) port;
    Map<String,Object> ctx = bp.getRequestContext();
    ctx.put("com.sun.xml.ws.connect.timeout", 3000);
    ctx.put("com.sun.xml.ws.request.timeout", 3000);
    return port;
  }

  private static String str(Object o){ return o==null? null : String.valueOf(o); }
  private static int num(Object o, int d){ try { return o==null? d : Integer.parseInt(String.valueOf(o)); } catch(Exception e){ return d; } }
  private static String maskCard(String c) { if (c==null) return null; String n=c.replaceAll("[^0-9]", ""); if (n.length()<4) return "****"; return "**** **** **** "+n.substring(n.length()-4); }
}
