# Generate Entity

JimmerBuddy can generate entity classes from the databases or ddl files.

## Add database or ddl file

1. Open the JimmerBuddy tool window.
2. Switch to `Database` tab.
3. Click `+` button to add a database or ddl file.

![20250525161526](https://s2.loli.net/2025/05/25/6LqW4NlZKJhR9uM.png)

4. Input some information about the database or ddl file.

![20250525162253](https://s2.loli.net/2025/05/25/C1REKkceOMHq2AP.png)

5. Click `OK` button to save the configuration.

![20250525162311](https://s2.loli.net/2025/05/25/N64wXcrDvsyY3qP.png)

6. Select the database or ddl file then right click it.
7. Select `Generate` from the context menu.
8. In the popup dialog, input the relative path and package name.

![20250525162405](https://s2.loli.net/2025/05/25/Bl3cTdeaEtIC49s.png)

9. Click `OK` button to generate the entity classes.

![20250525162454](https://s2.loli.net/2025/05/25/v85Khz2HL7Y6FQE.png)

## Configuration Entity Comments

Go to the `File and Code Templates` settings page and select `Jimmer`.

![20250525162739](https://s2.loli.net/2025/05/25/c6bPYIXEo9wvutA.png)

## Notes

- The generated entity classes will overwrite the existing ones with the same name.
- Tables without a primary key will not be generated.
- The `BaseEntity` will only be generated if the public fields are present in all tables.
- Join tables will only be treated as such if they have no primary key and only two fields.