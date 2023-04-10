LiBot Core ==========

Note: if you're looking to build and/or run LiBot, clone https://git.zajc.eu.org/libot/authoritative.git instead. It's the parent project.

This module contains the core of LiBot; everything related to managing the API state, invoking commands, scanning the classpath, managing shreds, and more. It also runs a read-only management socket server that can be used to read information about the bot externally.

Module data:
	Providers --------------
	- ConfigurationProvider
	- CustomizationsProvider
	
	Listeners --------------
	- EventLogListener
	- EventWaiterListener
	- MessageListener
	- ShredClashListener
	
	Commands ---------------
	(none)
