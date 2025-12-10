# core-plugin-base

<h1>How to use</h1>

<ol>
    <li>Init ServiceRegistry: <code>ServiceRegistry.init()</code></li>
    <li>To use any service: <code>ServiceRegistry.user().getUser()</code></li>
</ol>

<b>Note: </b> when using database configuration you need to init the connection with the following: <code>StorageManager.init()</code>