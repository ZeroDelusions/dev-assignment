package com.zero_delusions.dev_assignment.core.database.utils

import com.zero_delusions.dev_assignment.core.database.table.UserData
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration

object HibernateUtils {
    @JvmField
    val sessionFactory = buildSessionFactory()

    private fun buildSessionFactory(): SessionFactory {
        val configuration = Configuration()

        configuration.addAnnotatedClasses(UserData::class.java)

        val username = System.getenv("DB_USERNAME")
        val password = System.getenv("DB_PASSWORD")
        configuration.setProperty("hibernate.connection.username", username)
        configuration.setProperty("hibernate.connection.password", password)

        return configuration.buildSessionFactory()
    }
}