package com.zero_delusions.dev_assignment.core.database.utils

import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration

object HibernateUtils {
    @JvmField
    val sessionFactory = buildSessionFactory()

    private fun buildSessionFactory(): SessionFactory {
        val configuration = Configuration()

        val username = System.getenv(System.getenv("DB_USERNAME"))
        val password = System.getenv(System.getenv("DB_PASSWORD"))
        configuration.setProperty("hibernate.connection.username", username)
        configuration.setProperty("hibernate.connection.password", password)

        return configuration.buildSessionFactory()
    }
}