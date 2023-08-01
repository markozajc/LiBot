LiBot ==========

LiBot (https://libot.eu.org/) is a multi-purpose Discord bot. This is the
parent artifact encompassing all other Maven modules.

Get LiBot: . . . . . . . . . . . .  https://libot.eu.org/get
Or try it out in the support guild: https://discord.gg/asDUrbR


Building LiBot --------------
    Prerequisites:
       - Maven 3.2.5 or up (https://maven.apache.org/download.cgi)
       - JDK 17 or up (https://adoptium.net/)
       - qalculate-helper (https://git.zajc.eu.org/libot/qalculate-helper.git,
         optional if you want *calculator from module-utilities to work)

    Building:
       Building LiBot should be rather easy:
       
          $ git clone git://git.zajc.eu.org/libot/authoritative.git libot
          $ cd libot
          $ mvn clean package -DoutputLocation=/output/path

       (change /output/path to your preferred output destination) 
       If you would only like to build one module:
       
          ...
          $ mvn clean package -DoutputLocation=/output/path -pl 'module' -am

    Setting up the environment:
       LiBot takes configuration from environment variables, which means they
       need to be set up before running it. The following variables are
       required:
          - BOT_PREFIX: the default command prefix used by the bot
          - DATA_TYPE: the way LiBot stores runtime data. "file" for
                       persistent storage or "memory" to keep data in RAM.
          - DATA_PATH: a directory where persistent data is kept if DATA_TYPE
                       is "file"
          - SHRED_TOKEN_name: discord API tokens for shreds. You can define
                              multiple shreds by changing "name" to the
                              desired shred name, eg. SHRED_TOKEN_APOLLO for a
                              shred named APOLLO
       The following environment variables are optional:
         - BOT_SYSADMINS: a comma-separated list of discord user IDs for the
                          bot's administrators. Administrators are permitted
                          to run commands from 'module-administrative'. They
                          are also sent any uncaught exception stacktraces.
         - RESOURCE_GUILDS: a comma-separated list of "resource guilds", guilds
                            containing emoji used by other commands. Resource
                            guilds exempt from shred clash resolution, and
                            warning messages are issued if any of the shreds is
                            not in a particular resource guild.
         - QALCULATE_HELPER_PATH: path to the built qalculate-helper binary.
                                  Required for *calculator from module-utils to
                                  work.
         - QALCULATE_HOME_PATH: path to the home of the user running
                                qalculate-helper. Required for exchange rate
                                updating to work.
         - GOOGLE_TOKENS: google search API tokens. Required for *google of
                          module-search to work.
         - YOUTUBE_EMAIL: a youtube account's email. LiBot may not play
                          age-restricted youtube videos without this, but for
                          some reason it works for me, so it may be geolocked.
         - YOUTUBE_PASSWORD: a youtube account's password. Same reason as for
                             YOUTUBE_EMAIL.
         - MANAGEMENT_PORT: the port that LiBot's read-only management server
                            should run on. It has a couple of functions such
                            as choosing the best shred ID to authorize and
                            giving out stats.

    Running LiBot:
       The easiest way to run LiBot is to run it directly with Maven:
       
          $ ./run
          
       This will also take care of building LiBot and setting up the
       environment by reading it from .env in key=value pairs (so make sure you
       create that file first).
       If you would instead like to run built module jars, you need to put them
       on the classpath and run the libot.Main class:
       
          $ java -cp \
             /modules/core.jar:\
             /modules/module-first.jar:\
             /modules/module-second.jar:\
             /modules/module-last.jar\
             libot.Main

       You can specify as many modules as you want, while core.jar is mandatory.
