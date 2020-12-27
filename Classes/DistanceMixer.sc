DistanceMixer {
	var >numviews, <bufferDict, <>distMin, <>distMax, <>wet, <>out, <>encoder, <>group, <>fxsend;

	*new {|numviews = 10, bufferDict, distMin = 1, distMax = 30, wet = 0.85, out, encoder, group, fxsend|

		var win, views, title, plybtn, bufpop, bufnumbox, buftxt, changebtn, distlabel, distsld, distbx, thetalabel, thetasld, thetabx, philabel, phisld, phibx, ratelabel, ratesld, ratebx;

		var synthList, keys;
		var synthdef;
		var distanceSpec = ControlSpec(distMin, distMax, \lin, 0.0, units: "meters");
		var thetaSpec = ControlSpec(pi, -pi, \lin, 0.0, units: "radians");
		var phiSpec = ControlSpec(-pi/2, pi/2, \lin, 0.0,  units: "radians");
		var rateSpec = ControlSpec(0.125, 8, \exp, 0, 1, 1);
		var dialogbtn, dialogbtnLay, viewLay;

		synthList = Array.fill(numviews);
		keys = bufferDict.keys.asArray;

		synthdef = 	SynthDef(\distance, { |distance = 1, rate = 1, buf, spos = 0.0, theta = 0, phi = 0, gate = 1, amp = 1, fxout|
			var amplitude, azimuth, sig, foa, mix, numChans;

			numChans = encoder.numInputs;

			amplitude = distance.reciprocal.squared;
			//amplitude = distance.reciprocal;
			mix = distance.linlin(distMin, distMax, 1.0, wet);
			azimuth = distance.linlin(distMin, distMax, pi/2, 0);

			sig = PlayBuf.ar(numChans, buf, rate, 1.0, spos, loop:1) * amplitude;
			sig = DistanceFilter.ar(distance, sig);
			sig = sig * EnvGen.kr(Env.cutoff, gate, doneAction: Done.freeSelf);

			foa = FoaEncode.ar(sig, encoder);
			foa = FoaTransform.ar(foa, \push, azimuth, theta, phi);
			foa = foa * amp;

			Out.ar(out, foa);
			Out.ar(fxout, foa * (1 - mix));

		}, [0.1, 0.1, 0, 0, 0.1, 0.1, 0, 0.1, 0.1]
		).add;

		win = Window("Distance Mixer", 100@700, scroll: false).front;
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
					synthList.put(n,Synth(\distance, [
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
				if(btn.value == 0) {synthList[n].set(\buf, bufferDict[keys[bufpop[n].value].asSymbol][bufnumbox[n].value])} {};
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

		dialogbtn = 3.collect{|n|
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

		viewLay = HLayout(*views);
		dialogbtnLay = HLayout(HLayout(*dialogbtn), []);

		win.layout_(VLayout(viewLay, dialogbtnLay));

		^super.newCopyArgs(numviews, bufferDict, distMin, distMax, wet, out, encoder, group, fxsend)//.modWindow(ratesld[0].value)
	}

	/*	modWindow {|arga|
	var window = Window("", 400@400).front;
	Slider(window).value_(arga);

	}*/


}

