# Expansion-bungee-folia

Fork of BungeeExpansion 2.3 with Folia/Canvas support while retaining Bukkit/Paper support.

## Features

- `%bungee_total%`, `%bungee_all%`, `%bungee_<server>%`
- Same PlaceholderAPI identifier: `bungee`
- Same `check_interval` setting (default: 30 seconds)
- BungeeCord plugin messaging (`GetServers`, `PlayerCount`)
- Folia-safe scheduling via GlobalRegionScheduler and EntityScheduler
- Compatible scheduling path for standard Bukkit/Paper

## Build

Requires Java 21 and Gradle. Run:

```bash
gradle build
```

The JAR will be located at `build/libs/Expansion-bungee-folia.jar`.

## Installation

Place the built JAR in `plugins/PlaceholderAPI/expansions/`, remove the original
`Expansion-bungee.jar` to avoid an identifier collision, and restart the server.
Requires a proxy configured to support the BungeeCord plugin messaging channel.
At least one player must be online on the backend server for proxy messaging.

Original BungeeExpansion authors retain copyright in their code.
Distributed under the GPL-3.0 license; see `LICENSE`.
