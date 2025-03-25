package com.zero_delusions.dev_assignment.core.database.utils;

import com.zero_delusions.dev_assignment.core.database.table.UserData;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

public final class HibernateUtils {

    public static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        Configuration configuration = new Configuration();

        configuration.addAnnotatedClass(UserData.class);

        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        configuration.setProperty("hibernate.connection.username", username);
        configuration.setProperty("hibernate.connection.password", password);

        return configuration.buildSessionFactory();
    }

    @FunctionalInterface
    public interface SessionAction<T> {
        T execute(Session session);
    }

    public static <T> T executeWithSession(SessionAction<T> action) {
        Session session = sessionFactory.openSession();
        Transaction transaction = null;

        try {
            transaction = session.beginTransaction();
            T result = action.execute(session);
            transaction.commit();
            return result;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }
}