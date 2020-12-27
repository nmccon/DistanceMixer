DistanceFilter {
	*ar { |distance = (100), in |

		var freq = 	freq = ( 100000/distance).clip(20, 100000);
		^ OnePole.ar(in, (-2pi * (freq/SampleRate.ir)).exp);
	}
}
