# PowerTool &mdash; Bind commands to items #

Originally requested by InflamedSebi [in this thread](http://forums.bukkit.org/threads/powertool-single.39309/).

PowerTool allows you to bind commands to your in-hand items. Any
item may be assigned commands for both left-click and right-click. Bound
commands remain until you manually clear the binding.

Bound commands may also include a special player "token," `%p` by default, which
will be replaced by the name of the player you left-click or right-click on.

As of 0.9.4, bound commands may also include special coordinate tokens: `%x`,
`%y`, `%z`, and finally `%Y`. These are replaced with the X, Y, and Z
coordinates of the currently targeted block (up to 100 blocks away!) Note that
`%Y` (capital-Y) is special in that it is the Y-coordinate of the nearest air
block directly *above* the targeted block.

Please post bugs and/or feature requests as [dev.bukkit.org tickets](http://dev.bukkit.org/server-mods/powertool/tickets/).

## Commands ##

There's only one command, `/powertool` which may also be abbreviated `/pt`
or `/ptool`:

*   `/powertool left <the-command-to-bind>` &mdash; Binds the command (and its
    arguments) to the left-click action of the current item. The command must
    not include the first slash. This may also be abbreviated as
    `/powertool l`.
*   `/powertool right <the-command-to-bind>` &mdash; Binds the command (and its
    arguments) to the right-click action of the current item. This may also be
    abbreviated as `/powertool r`.
*   `/powertool left` &mdash; With no command to bind, the currently-bound left-click
	command is cleared from the current item. Use `/powertool right` to clear the
	right-click command.
*   `/powertool list` &mdash; Displays all player-defined power tools for the
    current player.
*   `/powertool clear` &mdash; Clears all bound commands from the current item.
*   `/powertool clear <item-id-or-name>` &mdash; Clears all bound commands from the
    specified item.
*   `/powertool clear -a` &mdash; Clears all bound commands from all items.
*   `/powertool on` &mdash; Enable power tools for this session (default is on).
*   `/powertool off` &mdash; Disable power tools for this session.
*   `/powertool toggle` &mdash; Toggle power tools availability. Can also be
    abbreviated as `/powertool t`.
*   `/powertool reload` &mdash; Re-reads config.yml.

## Permissions ##

PowerTool only supports Bukkit permissions (aka Superperms). By default, ops
have all permissions.

*   `powertool.use` &mdash; Allows setting and using power tools.
*   `powertool.reload` &mdash; Allows use of `/powertool reload`

Additionally, per-group power tool limits may be defined using permissions. See
the `options` section in config.yml for details.

## Global, Administrator-defined Binds ##

By editing config.yml, the server administrator can define binds for use by
anyone with the `powertool.use` permission.

For example:

    powertools:
	    string:
		    left-click: plugins
			right-click: version

The item ID ("string" in the above example) may be an integer item ID or a
name. If using an integer item ID, you must quote it as a string, e.g. `'287'`
for "string." If using a name, you must use a standard Bukkit material name
([found here](https://github.com/Bukkit/Bukkit/blob/master/src/main/java/org/bukkit/Material.java)). For
example, for golden apples, you may use one of:

*   `GOLDEN_APPLE`
*   `'golden apple'` (since it has a space, it must be quoted)
*   `goldenapple`

## Examples ##

*   Promote and demote users using the left-click and right-click actions of the
    current item:

    `/powertool l promote %p`

    `/powertool r demote %p`

*   Bind WorldEdit's copy & paste commands. Remember that many WorldEdit
	commands normally start with two slashes, but you must always omit the first
	slash.

    `/powertool left /copy`
	
	`/powertool right /paste`

## License & Source ##

PowerTool is released under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Sources may be found on GitHub:

*   [PowerTool](https://github.com/ZerothAngel/PowerTool)
*   [ToHPluginUtils](https://github.com/ZerothAngel/ToHPluginUtils)

Development builds may be found on my continous integration site:

*   [PowerTool](http://ci.tyrannyofheaven.org/job/PowerTool/lastSuccessfulBuild/org.tyrannyofheaven.bukkit$PowerTool/) (Requires ToHPluginUtils.jar)
*   [PowerTool-standalone](http://ci.tyrannyofheaven.org/job/PowerTool-standalone/lastSuccessfulBuild/org.tyrannyofheaven.bukkit$PowerTool/) (includes ToHPluginUtils, like the version distributed on dev.bukkit.org)

## To Do ##

*   Allow setting/clearing global power tools from the command line (for those
    with the proper permission, of course).
*   In the case of global power tools, allow permissions to control which power
    tools may be used. (Also supporting some sort of wildcard permission...)
*   Allow player-specific power tools to override global ones.
