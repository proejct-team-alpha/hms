package com.smartclinic.hms.admin.dashboard;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public class AdminDashboardStatsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public long countReservationsByDate(LocalDate reservationDate) {
        Long result = entityManager.createQuery(
                        "select count(r) from Reservation r where r.reservationDate = :reservationDate",
                        Long.class
                )
                .setParameter("reservationDate", reservationDate)
                .getSingleResult();
        return result == null ? 0L : result;
    }

    public long countAllReservations() {
        Long result = entityManager.createQuery(
                        "select count(r) from Reservation r",
                        Long.class
                )
                .getSingleResult();
        return result == null ? 0L : result;
    }

    public long countActiveStaff() {
        Long result = entityManager.createQuery(
                        "select count(s) from Staff s where s.active = true",
                        Long.class
                )
                .getSingleResult();
        return result == null ? 0L : result;
    }

    public long countLowStockItems() {
        Long result = entityManager.createQuery(
                        "select count(i) from Item i where i.quantity < i.minQuantity",
                        Long.class
                )
                .getSingleResult();
        return result == null ? 0L : result;
    }
}
