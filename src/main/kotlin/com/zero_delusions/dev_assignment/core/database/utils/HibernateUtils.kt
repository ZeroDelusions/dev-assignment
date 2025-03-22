package com.zero_delusions.dev_assignment.core.database.utils

import com.zero_delusions.dev_assignment.core.database.table.UserData
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.Transaction
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

    fun <T> executeWithSession(action: (Session) -> T): T {
        val session = sessionFactory.openSession()
        var transaction: Transaction? = null

        try {
            transaction = session.beginTransaction()
            val result = action(session)
            transaction.commit()
            return result
        } catch (e: Exception) {
            transaction?.rollback()
            throw e
        } finally {
            session.close()
        }
    }
}