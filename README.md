# PowerTool &mdash; Bind commands to items #

Originally requested by InflamedSebi [in this thread](http://forums.bukkit.org/threads/powertool-single.39309/).

PowerTool allows you to bind commands to your in-hand items. Any item may be assigned commands for both left-click and right-click. Bound commands remain until you manually clear the binding.

Please post bugs and/or feature requests as [dev.bukkit.org tickets](http://dev.bukkit.org/server-mods/powertool/tickets/).

## Features ##

*   Bound commands may include special tokens which are appropriately substituted when the item is used. Currently, these are:

<table>
  <tr>
    <th>Token</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>%p</td>
    <td>The name of the player you left-clicked or right-clicked.</td>
  </tr>
  <tr>
    <td>%x</td>
    <td>X-coordinate of the block targeted by your crosshairs</td>
  </tr>
  <tr>
    <td>%y</td>
    <td>Y-coordinate of the block targeted by your crosshairs</td>
  </tr>
  <tr>
    <td>%z</td>
    <td>Z-coordinate of the block targeted by your crosshairs</td>
  </tr>
  <tr>
    <td>%Y</td>
    <td>Y-coordinate of the nearest <i>air block</i> <b>above</b> the block targeted by your crosshairs</td>
  </tr>
</table>

All of these tokens are configurable.

*   Works in creative mode. Right-clicking and left-clicking *players* in creative mode will generally work, even when far away (up to 100 blocks).

*   Ability to define global power tools. These are administrator-defined binds that can be made available to whoever you want (based on permissions).

*   Fine-grained control over the use of power tools via permissions.

*   Global power tools may grant temporary permissions or even be run as the console. Needless to say, these features should be used with care.

*   The number of player-created binds may be limited based on group/permission.

*   Items may be further differentiated by custom display name (e.g. when given one via Anvils). Note this option is off by default, enable it in the configuration file.

## Commands ##

There's only one command, `/powertool` which may also be abbreviated `/pt` or `/ptool`:

*   `/powertool left <the-command-to-bind>` &mdash; Binds the command (and its arguments) to the left-click action of the current item. The command must not include the first slash. This may also be abbreviated as `/powertool l`.
*   `/powertool right <the-command-to-bind>` &mdash; Binds the command (and its arguments) to the right-click action of the current item. This may also be abbreviated as `/powertool r`.
*   `/powertool left` &mdash; With no command to bind, the currently-bound left-click command is cleared from the current item. Use `/powertool right` to clear the right-click command.
*   `/powertool list` &mdash; Displays all player-defined power tools for the current player.
*   `/powertool clear` &mdash; Clears all bound commands from the current item.
*   `/powertool clear <item-id-or-name>` &mdash; Clears all bound commands from the specified item.
*   `/powertool clear -a` &mdash; Clears all bound commands from all items.
*   `/powertool on` &mdash; Enable power tools for this session (default is on).
*   `/powertool off` &mdash; Disable power tools for this session.
*   `/powertool toggle` &mdash; Toggle power tools availability. Can also be abbreviated as `/powertool t`.
*   `/powertool reload` &mdash; Re-reads config.yml.

## Permissions ##

PowerTool only supports Bukkit permissions (aka Superperms). By default, ops have all permissions.

*   `powertool.use` &mdash; Allows the use and creation of power tools. (Has `powertool.use.*` and `powertool.create` as child permissions.)
*   `powertool.use.*` &mdash; Allows the use of all power tools.
*   `powertool.use.<item-id>` &mdash; Allows the use of a specific power tool identified by **item-id**. **item-id** may be numeric (e.g. **322** for golden apple), or the [Bukkit Material name](https://github.com/Bukkit/Bukkit/blob/master/src/main/java/org/bukkit/Material.java) with or without underscores (e.g. **GOLDEN_APPLE** or **GOLDENAPPLE**). Note that Bukkit permissions are not case-sensitive, so both `powertool.use.golden_apple` and `powertool.use.GOLDEN_APPLE` work fine.
*   `powertool.create` &mdash; Allows the creation of personal power tools. A player must be given this permission in order to use the left/right/list/clear subcommands.
*   `powertool.reload` &mdash; Allows use of `/powertool reload`

Additionally, per-group power tool limits may be defined using permissions. See the `options` section in config.yml for details.

## Global, Administrator-defined Binds ##

By editing config.yml, the server administrator can define binds for use by anyone with the `powertool.use` permission.

For example:

    powertools:
	    string:
		    left-click: plugins
			right-click: version

The item ID ("string" in the above example) may be an integer item ID or a name. If using an integer item ID, you must quote it as a string, e.g. `'287'` for "string." If using a name, you must use a standard Bukkit material name ([found here](https://github.com/Bukkit/Bukkit/blob/master/src/main/java/org/bukkit/Material.java)). For example, for golden apples, you may use one of:

*   `GOLDEN_APPLE`
*   `'golden apple'` (since it has a space, it must be quoted)
*   `goldenapple`

## Examples ##

*   Promote and demote users using the left-click and right-click actions of the current item:

    `/powertool l promote %p`

    `/powertool r demote %p`

*   Bind WorldEdit's copy & paste commands. Remember that many WorldEdit commands normally start with two slashes, but you must always omit the first slash.

    `/powertool left /copy`
	
	`/powertool right /paste`

## License & Source ##

PowerTool is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Sources may be found on GitHub:

*   [PowerTool](https://github.com/ZerothAngel/PowerTool)
*   [ToHPluginUtils](https://github.com/ZerothAngel/ToHPluginUtils)

Development builds of this project can be acquired at the provided continuous integration server. 
These builds have not been approved by the BukkitDev staff. Use them at your own risk.

*   [PowerTool](http://ci.tyrannyofheaven.org/job/PowerTool/) (Requires ToHPluginUtils.jar)
*   [PowerTool-standalone](http://ci.tyrannyofheaven.org/job/PowerTool-standalone/) (includes ToHPluginUtils, like the version distributed on dev.bukkit.org)

## To Do ##

*   Allow setting/clearing global power tools from the command line (for those with the proper permission, of course).
*   Allow player-specific power tools to override global ones.
*   Allow global power tool flags (e.g. run-as-console) to be assigned to each individual action.
