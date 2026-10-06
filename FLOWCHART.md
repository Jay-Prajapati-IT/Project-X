# Flowcharts

These are Mermaid diagrams. View them in VS Code with the "Markdown Preview Mermaid Support" extension, or on GitHub.

## 1. App structure
```mermaid
flowchart TD
    Launch[Open app] --> Home[Home dashboard<br/>today's habits, todos, reminders, birthdays]
    Home --> Habits
    Home --> Todo
    Home --> Reminders
    Home --> Birthdays
    Home --> Lock{Biometric unlock}
    Lock -->|success| Vault[Password Vault]
    Lock -->|success| Locker[File Locker]
    Lock -->|fail| Home
    Home --> Settings[Settings / Backup]
```

## 2. Data flow (every screen works like this)
```mermaid
flowchart LR
    UI[Compose Screen] -->|user action| VM[ViewModel]
    VM -->|insert/update| DAO[Room DAO]
    DAO --> DB[(Encrypted SQLite)]
    DB -->|Flow updates| VM
    VM -->|StateFlow| UI
```

## 3. Reminder / alarm lifecycle
```mermaid
flowchart TD
    A[User saves reminder] --> B[Save in DB]
    B --> C[AlarmScheduler sets exact alarm]
    C --> D{Time reached}
    D --> E[AlarmReceiver shows notification]
    E --> F{Repeats?}
    F -->|yes| C
    F -->|no| G[Mark done]
    R[Phone reboots] --> S[BootReceiver] --> T[Read all enabled reminders] --> C
```

## 4. Birthday check
```mermaid
flowchart TD
    W[WorkManager daily job] --> Q[Query birthdays for today and today + N days]
    Q --> N{Any match?}
    N -->|yes| P[Notification: 'Rahul's birthday in 2 days']
    N -->|no| X[Do nothing]
```

## 5. Vault / Locker unlock
```mermaid
flowchart TD
    O[Open Vault] --> BP[BiometricPrompt]
    BP -->|ok| K[Keystore releases key]
    K --> D[Decrypt and show entries/files]
    D --> BG{App in background > 1 min?}
    BG -->|yes| L[Lock again, clear from memory]
    BP -->|fail| Back[Back to Home]
```

## 6. Backup
```mermaid
flowchart LR
    E[Export] --> PW[Enter backup password] --> Z[Zip DB + locked files] --> ENC[Encrypt with password] --> SAF[Save via system file picker]
    I[Import] --> PICK[Pick file] --> PW2[Enter password] --> DEC[Decrypt] --> RESTORE[Replace data, reschedule alarms]
```
