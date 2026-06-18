## Gym CRM Project

Spring 6.1.x — потребує Java 17+, що в нас і є. Беремо тільки spring-context (тягне за собою core, beans, aop) + spring-test.

Без Spring Boot — як вимагає завдання («Spring core»).

OpenCSV — додав для зручного парсингу CSV (вимога «initialize storage from file»). Це краще, ніж парсити рядки руками — менше багів.

Surefire plugin — щоб JUnit 5 тести точно запускались через Maven.
