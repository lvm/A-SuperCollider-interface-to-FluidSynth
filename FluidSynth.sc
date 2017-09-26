/*
        FluidSynth class

        (c) 2017 by Mauro <mauro@sdf.org>, Cian O'Connor <cian.oconnor@gmail.com>
        http://cyberpunk.com.ar/

        A very basic fluidsynth "front-end".
        Reference:
        * https://sourceforge.net/p/fluidsynth/wiki/FluidSettings/

        Note: Requires `fluidsynth` installed in the system.
*/

FluidSynth {
  classvar fluidsynth; // Holds the singleton
  classvar <fluidsynth_bin; // command line location on this computer
  classvar <valid_audio_servers; // make sure the server matches.

  var audio_server, channels, commands_file;
  var fluidsynth_args;
  var fluidsynth_pipe;

  *initClass{
    fluidsynth_bin = "which fluidsynth".unixCmdGetStdOut.replace(Char.nl, "").asString;
    valid_audio_servers = [\alsa, \file, \jack, \oss, \pulseaudio];
  }

  *new {
    |audio_server channels commands_file|
    // singleton pattern
    if(fluidsynth.isNil){
      fluidsynth = super.new;
      fluidsynth.init(audio_server, channels, commands_file);
    }
    ^fluidsynth;
  }

  init {
    |audio_server=\jack, channels=16, commands_file=""|
    var audioServer, chan, cmds;

    // also, if audioServer is jack autoconnect.
    audioServer = if (audio_server==\jack){
      " -j -a " ++ audio_server;
    }{
      " -a " ++ audio_server;
    };

    chan = if (channels >=16 && channels <= 256){
      " -K " ++ channels
    }{
      error("channels should be an integer between 16 and 256");
    };

    cmds = "";
    if (File.exists(commands_file.standardizePath)){
        " -f " ++ commands_file.standardizePath
    };

    fluidsynth_args = " -sl" ++ audioServer ++ chan ++ " " ++ cmds;
    fluidsynth_pipe = Pipe.new("% %".format(fluidsynth_bin, fluidsynth_args), "w");
    "FluidSynth is running!".postln;
  }

  pr_send {
    |message|
    fluidsynth_pipe.write("%\n".format(message));
    fluidsynth_pipe.flush;
  }

  stop {
    fluidsynth_pipe.close;
    FluidSynth.pr_close;
    "FluidSynth is stopped!".postln;
  }

  /* Make sure that fluidsynth is set to nil once it's stopped so it can be reopened later */
  *pr_close{
    fluidsynth = nil;
  }

  setGain {
    |gain|

    pr_send(format("\ngain %", gain.asFloat.clip(0.01, 4.99)));
  }

  listChannels {
    pr_send("\nchannels");
  }

  listSoundfonts {
    pr_send("\nfonts");
  }

  listInstruments {
    |soundfont|

    pr_send(format("\ninst %", soundfont));
  }

  loadSoundfont {
    |soundfont|

    if (soundfont.isNil) {
      Error("TO_DO").throw;
    };

    pr_send(format("\nload %", soundfont));
  }

  unloadSoundfont {
    |soundfont|

    if (soundfont.isNil) {
      Error("TO_DO").throw;
    };

    pr_send(format("\nunload %", soundfont));
  }

  selectInstruments {
    |instruments|
    var select_cmd = "";
    var values;

    if (instruments.isNil.not and: (instruments.isKindOf(Array))) {
      instruments.collect {
        |inst|
        if (inst.isKindOf(Dictionary)) {
          values = [inst.at(\chan), inst.at(\sfont), inst.at(\bank), inst.at(\prog)];
          select_cmd = select_cmd ++ format("\nselect % % % %", *values);
        }
      };
    };

    pr_send(select_cmd);
  }
}
