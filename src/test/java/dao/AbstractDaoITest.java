package dao;

import config.TestPersistenceConfig;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for DAO integration tests.
 * Boots a Spring context with an in-memory H2 database and wraps each
 * test in a transaction that is rolled back afterwards (test isolation).
 */
@SpringJUnitConfig(TestPersistenceConfig.class)
@Transactional
public abstract class AbstractDaoITest {

    @Autowired
    protected SessionFactory sessionFactory;

    /** Current Spring-managed Hibernate session for the running test transaction. */
    protected Session getSession() {
        return sessionFactory.getCurrentSession();
    }
}