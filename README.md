<h1>What is core-plugin-base?</h1>

It is a base library intended to be used in future Hytale plugins. It provides common tools and infrastructure that do not depend on the Hytale API.

The current core library includes the following features:

<ul> <li>Users</li> <li>Roles</li> <li>Permissions</li> </ul>

For data persistence, you can use either a database or configuration files.
The database option is more solid and thread-safe, while configuration files may lead to synchronization issues (therefore, using a database is recommended).

You can define the storage type using the property <code>storageType</code>.
The necessary properties are defined as follows:

<pre><code> database: enabled: true host: localhost port: 3306 name: core user: root pass: secret file: path: permission-data.yml </code></pre> <h1>How to use</h1> <ol> <li>Initialize the <code>ServiceRegistry</code>: <code>ServiceRegistry.init()</code></li> <li>Use any service through the registry, for example: <code>ServiceRegistry.users().getUser()</code></li> </ol>

<b>Note:</b> When using a database configuration, you must initialize the connection by calling:
<code>StorageManager.init()</code>