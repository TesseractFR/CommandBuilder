# CommandBuilder
Utility library for building command executors

# Usage

## Building commands
Building a new command is done by instantiating a new CommandBuilder
```java
CommandBuilder myCommand = new CommandBuilder("myCommand");
// Optional settings
myCommand.permission("myPermission") // Set a permission to the command
         .playerOnly(true) // Specify that a command cannot be performed via the console
         .description("My super command"); // Add a description to the command
```
To add a behavior to the command, call the command() method
```java
myCommand.command(env -> {
    // Command logic
    sender.sendMessage("You performed the command!");
});
```

## Arguments

To add arguments to your command, instantiate a new CommandArgument and pass it to .withArg()
```java
myCommand.withArg(new CommandArgument("myArg", Integer.class)
                  /*
                   * A command must have a supplier, which parses the input given by the sender
                   */
                  .supplier((input, env) -> Integer.parseInt(input))
                  /*
                   * You can catch exception during parsing to display a useful error message to the sender
                   * error() can be called several times to handle multiple exceptions
                   */
                  .error(NumberFormatException.class, "This number is not valid")
                  /*
                   * You can add custom tab completion for the argument
                   */
                  .tabCompletion((sender, env) -> List.of("1", "2", "3"));
```

You can chain multiple `.withArg()` to add arguments to the command

### Getting arguments

When an argument is parsed, it is stored in a CommandEnvironment, that holds all parsed argument of a command in execution. You can retrieve an argument by using the `get` method.
```java
// The CommandEnvironment is passed to the command function to get arguments during command logic
myCommand.command(env -> {
    // Command logic
    Player player = env.get("player", Player.class);
    float amount = env.get("myArg", Float.clas);
    player.giveMoney(amount);
});

// The CommandEnvironment is also passed to suppliers and tabCompletion of arguments. That way the parsing and tabCompletion of an argument can depend on the value of a previous argument
myCommand.withArg(new CountryCommandArgument("country")) // Custom argument class
         .withArg(new CommandArgument("city", City.class)
                  .supplier((cityName, env) -> {
                      City city = env.get("country", Country.class).getCity(cityName);
                      if (city == null)
                          throw new CityNotFoundException();
                      return city;
                  })
                  .error(CityNotFoundException.class, "There is no such city in this country")
                  .tabCompletion((sender, env) -> env.get("country", Country.class).getCities().map(City::getName)));
```

## Subcommands

Add one or more sub command by passing another CommandBuilder. TabCompletion, execution and environment will be propagated to subCommands.
```java
CommandBuilder mySubCommand = new CommandBuilder("mySubCommand");
myCommand.subCommand(mySubCommand);
// --> /myCommand arg1 arg2 ... mySubCommand subArg1 subArg2 ...
```

## Using commands

To execute a command
```java
public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label,
                             @Nonnull String[] args)
    {
        mySender.execute(sender, args);
        return true;
    }
```

To execute a tabCompletion
```java
@Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
                                      @NotNull String[] args)
    {
        return myCommand.tabComplete(sender, args);
    }
```

