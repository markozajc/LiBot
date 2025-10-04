LiBot Music Module ==========

A module providing commands and features allowing playback and management of music in voice channels. Using yt-cipher
(https://github.com/kikkia/yt-cipher) is recommended, but not yet required.

Module data:
	Providers --------------------------
	- [depends] CustomizationsProvider
	- MusicRestoreProvider
	
	Listeners --------------------------
	- MusicStateListener
	
	Commands ---------------------------
	- ClearQueueCommand
	- LoopCommand
	- PauseCommand
	- PlayCommand
	- PlayingCommand
	- QueueCommand
	- SeekCommand
	- ShuffleCommand
	- SkipCommand
	- StopCommand
	- YoutubePlayCommand