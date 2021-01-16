//DistanceMixer

DistanceMixer {
	var numviews, bufferDict, distMin, distMax, wet, out, encoder, group, fxsend;
	var synthDef, synthList, keys;
	var distanceSpec, thetaSpec, phiSpec, rateSpec;
	var win, views;
	var title, plybtn, bufpop, bufnumbox, buftxt, changebtn, distlabel, distsld, distbx;
	var thetalabel, thetasld, thetabx, philabel, phisld, phibx, ratelabel, ratesld, ratebx;
	var dialogbtn, dialogbtnLay, viewLay;

	*new { |numviews = 10, bufferDict, distMin = 1, distMax = 30, wet = 0.85, out, encoder, group, fxsend|
		^super.newCopyArgs(numviews, bufferDict, distMin, distMax, wet, out, encoder, group, fxsend).init
	}

	init {
		synthList = Array.fill(numviews);
		keys = bufferDict.keys.asArray;
		distanceSpec = ControlSpec(distMin, distMax, \lin, 0.0, units: "meters");
		thetaSpec = ControlSpec(pi, -pi, \lin, 0.0, units: "radians");
		phiSpec = ControlSpec(-pi/2, pi/2, \lin, 0.0,  units: "radians");
		rateSpec = ControlSpec(0.125, 8, \exp, 0, 1, 1);

		synthDef = SynthDef(\distance, { |distance = 1, rate = 1, buf, spos = 0.0, theta = 0, phi = 0, gate = 1, amp = 1, fxout|

			var sig, foa;
			var amplitude = distance.reciprocal.squared;
			var mix = distance.linlin(distMin, distMax, 1.0, wet);
			var azimuth = distance.linlin(distMin, distMax, pi/2, 0);

			sig = PlayBuf.ar(this.numInputs, buf, rate, 1.0, spos, loop:1) * amplitude;
			sig = DistanceFilter.ar(distance, sig);
			sig = sig * EnvGen.kr(Env.cutoff, gate, doneAction: Done.freeSelf);

			foa = FoaEncode.ar(sig, encoder);
			foa = FoaTransform.ar(foa, \push, azimuth, theta, phi);
			foa = foa * amp;

			Out.ar(out, foa);
			Out.ar(fxout, foa * (1 - mix));

		}, [0.1, 0.1, 0, 0, 0.1, 0.1, 0, 0.1, 0.1]
		).add;

		^this.makeGUI
	}

	numInputs { ^encoder.numInputs}

	makeGUI {

		win = Window("Distance Mixer", 100@700, scroll: true).front;
		win.onClose_({
			numviews.do({|n|
				synthList[n].free
			})
		});
		win.addFlowLayout();

		views = Array.fill(numviews, {|v|
			v = CompositeView(win,win.bounds)
			.resize_(2)
			.background_(if(v%2 == 0) {Color.new255(78, 91, 102)} {Color.new255(67, 77, 87)});
			v.layout_(VLayout());
		});

		title = numviews.collect{|n|
			StaticText(views[n])
			.string_("Synth "++n)
			.align_(\center)
			.stringColor_(Color.white)
		};

		plybtn = numviews.collect{|n|
			Button(views[n])
			.states_([["Start", Color.black, Color.white],["Stop", Color.black, Color.clear]])
			.action_({|btn|
				if(btn.value==1) {
					synthList.put(n, Synth(\distance, [
						\buf, bufferDict[keys[bufpop[n].value].asSymbol][bufnumbox[n].value],
						\out, out,
						\fxout, fxsend
					],group)
					);
					distsld[n].valueAction_(distsld[n].value);
					thetasld[n].valueAction_(thetasld[n].value);
					phisld[n].valueAction_(phisld[n].value);
					ratesld[n].valueAction_(ratesld[n].value);
					title[n].stringColor_(Color.green);
					//bufnumbox[n].valueAction_(0);
					synthList.postln;
				} {
					synthList[n].free;
					title[n].stringColor_(Color.white);
				}
			});
		};

		bufpop = numviews.collect{|n|
			PopUpMenu(views[n])
			.items_(keys)
			.value_(0)
			.action_({|pop|
				var size = bufferDict[keys[pop.value]].size-1;
				bufnumbox[n].clipHi_(size)
			})
		};

		bufnumbox = numviews.collect{|n|
			NumberBox(views[n])
			.valueAction_(0)
			.action_({|bx|
				var strg = bufferDict[keys[bufpop[n].value].asSymbol][bx.value].path.basename;
				buftxt[n].string_(strg)})
			.clipLo_(0);
		};

		buftxt = numviews.collect{|n|
			StaticText(views[n])
			.string_("File Name")
			.align_(\center)
			.stringColor_(Color.white)
		};

		changebtn = numviews.collect{|n|
			Button(views[n])
			.states_([["Change Buffer", Color.black, Color.white]])
			.action_({|btn|
				if(btn.value == 0) {
					synthList[n].set(\buf, bufferDict[keys[bufpop[n].value].asSymbol][bufnumbox[n].value])
				} {};
			})
		};

		//distance

		distlabel = numviews.collect{|n|
			StaticText(views[n])
			.string_("Distance")
			.align_(\center)
			.stringColor_(Color.white)
		};

		distsld = numviews.collect{|n|
			Slider(views[n])
			.background_(Color.gray(0.68))
			.orientation_(\vertical)
			.value_(distanceSpec.unmap(distMin))
			.action_({|v|
				synthList[n].set(\distance, distanceSpec.map(v.value));
				distbx[n].value_(distanceSpec.map(v.value));
			});
		};

		distbx = numviews.collect{|n|
			NumberBox(views[n])
			.value_(distanceSpec.unmap(distMin))
			.action_({|v|
				distsld[n].valueAction_(distanceSpec.unmap(v.value))
			});
		};

		//theta

		thetalabel = numviews.collect{|n|
			StaticText(views[n])
			.string_("Theta")
			.align_(\center)
			.stringColor_(Color.white)
		};

		thetasld = numviews.collect{|n|
			Slider(views[n])
			.orientation_(\horizontal)
			.value_(thetaSpec.unmap(0))
			.action_({|v|
				synthList[n].set(\theta, thetaSpec.map(v.value));
				thetabx[n].value_(thetaSpec.map(v.value));
			})
		};

		thetabx = numviews.collect{|n|
			NumberBox(views[n])
			.action_({|v|
				thetasld[n].valueAction_(thetaSpec.unmap(v.value))
			});
		};

		//phi

		philabel = numviews.collect{|n|
			StaticText(views[n])
			.string_("Phi")
			.align_(\center)
			.stringColor_(Color.white)
		};

		phisld = numviews.collect{|n|
			Slider(views[n])
			.orientation_(\horizontal)
			.value_(phiSpec.unmap(0))
			.action_({|v|
				synthList[n].set(\phi, phiSpec.map(v.value));
				phibx[n].value_(phiSpec.map(v.value))
			})
		};

		phibx = numviews.collect{|n|
			NumberBox(views[n])
			.action_({|v|
				phisld[n].valueAction_(phiSpec.unmap(v.value))
			})
		};

		//playback speed

		ratelabel = numviews.collect{|n|
			StaticText(views[n])
			.string_("Play Rate")
			.align_(\center)
			.stringColor_(Color.white)
		};

		ratesld = numviews.collect{|n|
			Knob(views[n])
			.color_([Color.gray(0.68), Color.black, Color.white])
			.value_(rateSpec.unmap(1))
			.action_({|v|
				synthList[n].set(\rate, rateSpec.map(v.value));
				ratebx[n].value_(rateSpec.map(v.value))
			})
		};

		ratebx = numviews.collect{|n|
			NumberBox(views[n])
			.value_(1)
			.action_({|v|
				ratesld[n].valueAction_(rateSpec.unmap(v.value))
			});
		};

		//load save reset

		dialogbtn = 4.collect{|n|
			Button(/*btncomp*/)
			.maxWidth_(60);
		};

		dialogbtn[0]
		.states_([["Save"]])
		.action_({
			Dialog.savePanel({|path|
				[
					distsld.collect{|n| n.value},
					thetasld.collect{|n| n.value},
					phisld.collect{|n| n.value},
					ratesld.collect{|n| n.value},
					bufpop.collect{|n| n.value},
					bufnumbox.collect{|n| n.value},
					plybtn.collect{|n| n.value}
				].writeTextArchive(path)
			})
		});

		dialogbtn[1]
		.states_([["Load"]])
		.action_({
			var values;
			Dialog.openPanel({|path|
				values = Object.readTextArchive(path[0]).flat;
				numviews.do{|n| distsld[n].valueAction = values[n]};
				numviews.do{|n| thetasld[n].valueAction = values[n+numviews]};
				numviews.do{|n| phisld[n].valueAction = values[n+(numviews *2)]};
				numviews.do{|n| ratesld[n].valueAction = values[n+(numviews *3)]};
				numviews.do{|n| bufpop[n].valueAction = values[n+(numviews *4)]};
				numviews.do{|n| bufnumbox[n].value = values[n+(numviews *5)]};
				numviews.do{|n| plybtn[n].valueAction = values[n+(numviews *6)]};
			}, multipleSelection: true)
		});

		dialogbtn[2]
		.states_([["Reset"]])
		.action_({
			numviews.collect{|n|
				distbx[n].valueAction_(distanceSpec.unmap(distMin));
				thetasld[n].valueAction_(thetaSpec.unmap(0));
				phisld[n].valueAction_(phiSpec.unmap(0));
				ratesld[n].valueAction_(rateSpec.unmap(1));
				bufpop[n].value_(0);
				bufnumbox[n].value_(0);
				plybtn[n].valueAction_(0);
			}
		});

				dialogbtn[3]
		.states_([["OSC"]])
		.action_({
			this.oscTest
			});

		viewLay = HLayout(*views);
		dialogbtnLay = HLayout(HLayout(*dialogbtn), []);

		win.layout_(VLayout(viewLay, dialogbtnLay));
	}

	//OSC: Reaper -> SC

	oscTest {
		var oscWin, oscWinLay, oscView, title;
		var playTxt, distTxt, bufTxt, thetaTxt, phiTxt, rateTxt;
		var playBtn, distBtn, bufBtn, thetaBtn, phiBtn, rateBtn;

		oscWin = Window("OSC Receive", 700@700, resizable: true, scroll: true)
		.alpha_(0.85)
		.front;

		oscView = Array.fill(numviews, {|v|
			v = CompositeView(oscWin, oscWin.bounds)
			.resize_(2)
			.background_(if(v%2 == 0) {Color.new255(78, 91, 102)} {Color.new255(67, 77, 87)});
			v.layout_(VLayout())
		});

		title = numviews.collect{|n|
			StaticText(oscView[n])
			.string_("Synth "++n)
			.stringColor_(Color.white)
			.align_(\center)

		};

		playTxt = numviews.collect{|n|
			EZText(
				oscView[n],
				16@20,
				"start",
				{|ez| ez.value.postln},
				labelWidth:30
			)
		};

		playBtn = numviews.collect{|n|
			Button(oscView[n], 16@20)
			.states_([
				["Map", Color.green],
				["Free", Color.red],
			])
			.action_({|btn|
				if(btn.value == 1) {
					OSCdef(playTxt[n], {|msg, time, addr, port = 51270|
						{plybtn[n].valueAction_(msg[1].value)}.defer;
					}, playTxt[n].value.asSymbol);
				} {
					OSCdef(playTxt[n]).free;
					{plybtn[n].value_(0)}.defer;
				}
			})
		};

		bufTxt = numviews.collect{|n|
			EZText(
				oscView[n],
				16@20,
				"buf",
				{|ez| ez.value.postln},
				labelWidth:30
			)
		};

		bufBtn = numviews.collect{|n|
			Button(oscView[n], 16@20)
			.states_([
				["Map", Color.green],
				["Free", Color.red],
			])
			.action_({|btn|
				if(btn.value == 1) {
					OSCdef(bufTxt[n], {|msg, time, addr, port = 51270|
						{changebtn[n].valueAction_(msg[1].value)}.defer;
					}, bufTxt[n].value.asSymbol);
				} {
					OSCdef(bufTxt[n]).free;
					{changebtn[n].value_(0)}.defer;
				}
			})
		};

		distTxt = numviews.collect{|n|
			EZText(
				oscView[n],
				16@20,
				"dist",
				{|ez| ez.value.postln},
				labelWidth:30
			)
		};

		distBtn = numviews.collect{|n|
			Button(oscView[n], 16@20)
			.states_([
				["Map", Color.green],
				["Free", Color.red],
			])
			.action_({|btn|
				if(btn.value == 1) {
					OSCdef(distTxt[n], {|msg, time, addr, port = 51270|
						{distsld[n].valueAction_(msg[1].value)}.defer;
					}, distTxt[n].value.asSymbol);
				} {
					OSCdef(distTxt[n]).free;
					{distsld[n].value_(0)}.defer;
				}
			})
		};

		thetaTxt = numviews.collect{|n|
			EZText(
				oscView[n],
				16@20,
				"theta",
				{|ez| ez.value.postln},
				labelWidth:30
			)
		};

		thetaBtn = numviews.collect{|n|
			Button(oscView[n], 16@20)
			.states_([
				["Map", Color.green],
				["Free", Color.red],
			])
			.action_({|btn|
				if(btn.value == 1) {
					OSCdef(thetaTxt[n], {|msg, time, addr, port = 51270|
						{thetasld[n].valueAction_(msg[1].value)}.defer;
					}, thetaTxt[n].value.asSymbol);
				} {
					OSCdef(thetaTxt[n]).free;
					{thetasld[n].value_(0)}.defer;
				}
			})
		};

		phiTxt = numviews.collect{|n|
			EZText(
				oscView[n],
				16@20,
				"phi",
				{|ez| ez.value.postln},
				labelWidth:30
			)
		};

		phiBtn = numviews.collect{|n|
			Button(oscView[n], 16@20)
			.states_([
				["Map", Color.green],
				["Free", Color.red],
			])
			.action_({|btn|
				if(btn.value == 1) {
					OSCdef(phiTxt[n], {|msg, time, addr, port = 51270|
						{phisld[n].valueAction_(msg[1].value)}.defer;
					}, phiTxt[n].value.asSymbol);
				} {
					OSCdef(phiTxt[n]).free;
					{phisld[n].value_(0)}.defer;
				}
			})
		};

		rateTxt = numviews.collect{|n|
			EZText(
				oscView[n],
				16@20,
				"rate",
				{|ez| ez.value.postln},
				labelWidth:30
			)
		};

		rateBtn = numviews.collect{|n|
			Button(oscView[n], 16@20)
			.states_([
				["Map", Color.green],
				["Free", Color.red],
			])
			.action_({|btn|
				if(btn.value == 1) {
					OSCdef(rateTxt[n], {|msg, time, addr, port = 51270|
						{ratesld[n].valueAction_(msg[1].value)}.defer;
					}, rateTxt[n].value.asSymbol);
				} {
					OSCdef(rateTxt[n]).free;
					{ratesld[n].value_(0)}.defer;
				}
			})
		};

		oscWinLay = HLayout(*oscView);
		oscWin.layout_(oscWinLay);
		oscWin.onClose_{OSCdef.freeAll};
	}

}

//DistanceFilter

DistanceFilter {
	*ar { | distance = (100), in |
		var freq = ( 100000/distance).clip(20, 100000);
		^ OnePole.ar(in, (-2pi * (freq/SampleRate.ir)).exp);
	}
}


/*
Eli Fieldsteel's 'makeBufDict instance method for PathName:
https://gist.github.com/elifieldsteel/396cd1326d3c981ba1fd2a3c47d90ea3
The DistanceMixer depends on this method to correctly access buffers via the GUI.
*/

+ PathName {

	//makeBufDict takes an instance of PathName (pointing to a folder)
	//and iterates over all items within. This method expects all
	//subfolders to either contain all audio files or all folders.
	//When a folder of subfolders is found, this method calls itself
	//recursively. When a folder of audio files is found, the method
	//adds a new key to a running Dictionary, using the name of the
	//parent folder, and places at that key an Array of Buffers. The
	//method assumes the default server is already running and warns
	//the user if this is not the case.
	makeBufDict {

		//dict is initally an empty dictionary, but is modified
		//as folders of soundfiles are found and added to dict.
		//This method is recursive in nature, and the modified
		//dictionary is passed back to the method on each
		//recursive evaluation.
		arg dict = Dictionary.new, warnPosted=false, mono=false;

		//Warn if the default server is not running. 'warnPosted'
		//exists to prevent multiple warnings.
		if(
			Server.default.serverRunning.not && warnPosted.not,
			{
				"Server not running".warn;
				warnPosted = true;
			}
		);

		if(
			//Check whether the folder contains folders or files
			this.entries.select(_.isFile).size == 0,

			//If contents are all folders, recur, passing the
			//running Dictionary and warn boolean in as arguments.
			{
				this.entries.do{
					|n|
					n.makeBufDict(dict, warnPosted, mono);
				}
			},

			//If contents are all files, add a new key to 'dict' using
			//the name of the parent folder, and place at that key
			//an Array of Buffers
			{
				var parentPath, bufs;
				parentPath = this.allFolders[this.allFolders.size-1].asSymbol;
				bufs = this.entries.collect(_.fullPath).collect{
					|i|
					if(
						mono,
						{Buffer.readChannel(Server.default,i,channels:[0])},
						{Buffer.read(Server.default,i)}
					);
				};
				dict.add(parentPath -> bufs);
			};
		);

		//return the instance of Dictionary.
		^dict;
	}
}

