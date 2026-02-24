# NewPlayerProtection

A Minecraft Paper plugin that gives new players temporary PvP protection. Players joining for the first time automatically receive a configurable protection period (default 72 hours) during which they cannot be attacked or attack others.

**Paper 1.21.4+ | Java 21**

## Features

- **Automatic Protection** — New players receive protection on first join. Returning protected players see remaining time on login.
- **Bidirectional PvP Blocking** — Protected players can't be attacked and can't attack others. Covers both melee and projectile damage.
- **Admin Controls** — Manually grant or remove protection, check any player's status, reload config.
- **Statistics & Leaderboards** — Tracks attacks blocked, attacks prevented per player, with top-10 leaderboards.
- **PlaceholderAPI Support** — Exposes protection status, time remaining, and all stats as placeholders.
- **YAML Persistence** — Protection data and statistics saved to file with automatic expiration cleanup.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/npp me` | Check your own protection status | `newplayerprotection.stats` |
| `/npp stats` | View your own statistics | `newplayerprotection.stats` |
| `/npp stats <player>` | View another player's stats | `newplayerprotection.stats.others` |
| `/npp stats top [stat]` | View top 10 leaderboard | `newplayerprotection.stats` |
| `/npp add <player>` | Grant protection to a player | `newplayerprotection.admin` |
| `/npp remove <player>` | Remove protection from a player | `newplayerprotection.admin` |
| `/npp check <player>` | Check a player's protection status | `newplayerprotection.admin` |
| `/npp reload` | Reload config and data | `newplayerprotection.admin` |

**Aliases:** `/newplayerprotection`, `/protection`

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `newplayerprotection.admin` | Reload, add/remove/check protection | OP |
| `newplayerprotection.bypass` | Attack and be attacked while protected | OP |
| `newplayerprotection.stats` | View own protection statistics | Everyone |
| `newplayerprotection.stats.others` | View other players' stats | OP |

## Configuration

```yaml
# Duration of new player protection in hours
protection-duration-hours: 72
```

All messages are fully customizable with `&` color code support in `config.yml`, including:
- Protection granted/expired notifications
- Attack blocked messages (both directions)
- Admin command feedback
- Status check responses

## Placeholders

Requires [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/).

| Placeholder | Description |
|-------------|-------------|
| `%newplayerprotection_stat_protected%` | "Yes" or "No" |
| `%newplayerprotection_stat_protection_time_remaining%` | Formatted time remaining |
| `%newplayerprotection_stat_attacks_blocked%` | Incoming attacks blocked |
| `%newplayerprotection_stat_attacks_prevented%` | Outgoing attacks prevented |
| `%newplayerprotection_global_total_attacks_blocked%` | Server-wide attacks blocked |
| `%newplayerprotection_global_total_players_protected%` | Currently protected count |
| `%newplayerprotection_global_total_players_ever_protected%` | Lifetime protected count |
| `%newplayerprotection_top_<stat>_<pos>%` | Leaderboard player name |
| `%newplayerprotection_topvalue_<stat>_<pos>%` | Leaderboard value |

## Dependencies

| Dependency | Required |
|------------|----------|
| [VersionAdapter](https://github.com/BekoLolek/VersionAdapter) | Yes |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | No |
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | No |

## Installation

1. Place `NewPlayerProtection.v1-BL.jar` in your server's `plugins/` folder.
2. Ensure `VersionAdapter.jar` is also in the `plugins/` folder.
3. Restart the server.
4. Edit `plugins/NewPlayerProtection/config.yml` to customize duration and messages.

## Part of the BekoLolek Plugin Ecosystem

Built by **Lolek** for the BekoLolek Minecraft network.
