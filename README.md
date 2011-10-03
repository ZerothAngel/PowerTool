# PowerTool &mdash; Bind commands to items #

Originally requested by InflamedSebi [in this thread](http://forums.bukkit.org/threads/powertool-single.39309/).

PowerTool allows you to temporarily bind commands to your in-hand items. Any
item may be assigned commands for both left-click and right-click. Bound
commands remain until you either log off or you manually clear the binding.

Bound commands may also include special player "token," `%p` by default, which
will be replaced by the name of the player you left-click or right-click on.

## Commands ##

There's only one command, `/powertool` which may also be abbreviated `/pt`:

*   `/powertool <the-command-to-bind>` &mdash; Binds the command (and its
    arguments) to the left-click action of the current item. The command must
    not include the first slash.
*   `/powertool -r <the-command-to-bind>` &mdash; Binds the command (and its
    arguments) to the right-click action of the current item.
*   `/powertool` &mdash; With no command to bind, the currently-bound left-click
	command is cleared from the current item. Use `/powertool -r` to clear the
	right-click command.
*   `/powertool -c` &mdash; Clears all bound commands from the current item.

## Permissions ##

The only (Superperms) permission node is `powertool.use` which allows the use of
the `/powertool` command. By default, ops have this permission.

(To change the default, edit plugin.yml in the JAR file.)

## Examples ##

*   Promote and demote users using the left-click and right-click actions of the
    current item:

    `/powertool promote %p`

    `/powertool -r demote %p`

*   Bind WorldEdit's copy & paste commands. Remember that many WorldEdit
	commands normally start with two slashes, but you must always omit the first
	slash.

    `/powertool /copy`
	
	`/powertool -r /paste`

## License & Source ##

PowerTool is released under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Sources may be found on GitHub:

*   [PowerTool](https://github.com/ZerothAngel/PowerTool)
*   [ToHPluginUtils](https://github.com/ZerothAngel/ToHPluginUtils)

## To Do ##

*  More substitutions. Perhaps %loc, which would be replaced by the location of
    the currently targeted block respectively.
*   Persistence &mdash; save powertools to database or at least let permanent
    ones be defined in config.yml.
