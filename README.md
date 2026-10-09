# Bank Management System

A desktop Bank Management System built with Java 17 (Swing) and MySQL (JDBC).

## Features

- Secure login with role selection (Admin / Customer)
- Admin dashboard: register customers & accounts, browse the customer directory, view the system audit log
- Customer portal: view balance, deposit, withdraw, transfer funds, and review transaction history
- The database and seed data are created automatically on first run

## Requirements

- JDK 17 or newer (`java -version` should print 17+)
- MySQL Server 8.x running locally

## How to run

On Windows, double-click **`run.bat`**. It compiles the sources and then launches the app.

Or from a terminal in the project folder:

```bat
javac -cp "lib\mysql-connector-j-8.3.0.jar" -d bin -sourcepath src src\App.java
java -cp "bin;lib\mysql-connector-j-8.3.0.jar" App
```

> The MySQL driver lives in `lib\` and must stay on the classpath (that is what the
> `-cp` option above does). Running plain `java App` without `-cp` will fail with
> "No suitable driver".

## Database configuration

Connection settings are resolved in this order (first one found wins):

1. JVM system property, e.g. `-Ddb.password=secret`
2. Environment variable
3. Built-in default

| Setting  | System property | Environment variable | Default |
|----------|-----------------|----------------------|---------|
| URL      | `db.url`        | `DB_URL`             | `jdbc:mysql://localhost:3306/bank_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| Username | `db.username`   | `DB_USER`            | `root` |
| Password | `db.password`   | `DB_PASSWORD`        | `paswd` |

Set your MySQL password before running, for example:

- Windows (PowerShell): `setx DB_PASSWORD yourpassword` (then reopen the terminal)
- Instead, edit the default directly in `src/db/DBConfig.java`

The `bank_db` database and its tables/seed data are created automatically on first run.
To set it up manually instead, run `schema.sql` once in MySQL.

## Default login

| Role     | Username     | Password  |
|----------|--------------|-----------|
| Admin    | `admin`      | `admin123`|
| Customer | `san`        | `user123` |
| Customer | `jane_smith` | `user123` |

## Project structure

```
src/
  App.java              entry point
  classes/              model classes (User, Admin, Customer, Account, Transaction, DTO)
  db/                   DBConfig + DatabaseOperations (JDBC access)
  ui/                   LoginFrame, AdminDashboard, CustomerDashboard
lib/                    mysql-connector-j-8.3.0.jar
run.bat                 compile + run
schema.sql              manual database setup script
```
