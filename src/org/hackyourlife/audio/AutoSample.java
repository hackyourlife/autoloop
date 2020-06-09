package org.hackyourlife.audio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hackyourlife.audio.analysis.Autoloop;
import org.hackyourlife.audio.analysis.Frequency;
import org.hackyourlife.audio.analysis.Loop;
import org.hackyourlife.audio.analysis.LoopTuner;
import org.hackyourlife.audio.analysis.MIDINames;
import org.hackyourlife.audio.analysis.PitchEstimator;
import org.hackyourlife.audio.analysis.SpectraLoop;
import org.hackyourlife.audio.dsp.FFT;
import org.hackyourlife.audio.io.riff.Riff;
import org.hackyourlife.audio.io.riff.RiffWave;
import org.hackyourlife.audio.io.riff.SampleChunk;
import org.hackyourlife.audio.io.riff.SampleChunk.SampleLoop;

public class AutoSample {
	private static final double ERROR_THRESHOLD = 0.005;

	private static void help() {
		String helpText = "Usage: Autoloop OPTIONS...\n" +
				"\n" +
				" -f file.wav   Input file in wav format (8/16/24bit integer, 32bit float)\n" +
				" -i 0.0        Skip that many seconds in the beginning [default: 0.0]\n" +
				" -s 4000       Step through the audio file in n sample steps [default: 4000]\n" +
				" -m 1.0        Minimum loop length of n seconds [default: 1.0]\n" +
				" -t 1000       Number of samples to check after loop end [default: 1000]\n" +
				" -w 1.0        Weighting factor to trade loop length vs loop quality\n" +
				"               [default: 1.0, meaning only quality is relevant]\n" +
				" -p 2          Number of concurrent threads for analysis [default: 2]\n" +
				" -o out.wav    Output file (trimmed + embedded loop points)\n" +
				" -k c#4        Root key (name) to embed in the output file [conflicts with -n]\n" +
				" -n 60         Root key (MIDI number) to embed in the output file\n" +
				"               [conflicts with -k; automatically estimated if no key is given]\n" +
				" -a            Use advanced analysis to detect long repeating patterns\n" +
				"\n" +
				"In normal mode, -t many samples are checked for equality after the loop point.\n" +
				"This typically works fine for simple sounds, but it fails to capture longer\n" +
				"structures like LFO modulations on pitch/filter/... That is where advanced mode\n" +
				"comes in: it uses a different analysis approach to find long repeating\n" +
				"structures in the sample and then tunes the loop point to be near the long\n" +
				"structure. This works for wave sequences, LFO modulated pitch/filter/..., but\n" +
				"it produces suboptimal results for simple waveforms.";
		System.out.println(helpText);
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		if(args.length == 0) {
			help();
			return;
		}

		String outfileName = null;
		RiffWave wav = null;
		int step = 4000;
		int tail = 1000;
		double weight = 1;
		double skipSec = 0;
		double minSec = 1;

		int threadcnt = 2;
		int key = -1;

		boolean useSpectrum = false;

		for(int i = 0; i < args.length; i++) {
			switch(args[i]) {
			case "-h":
			case "--help":
				help();
				return;
			case "-f":
				i++;
				if(i >= args.length) {
					System.out.println("Error: missing argument");
					return;
				}
				wav = loadWaveFile(args[i]);
				break;
			case "-i":
				i++;
				if(i >= args.length) {
					System.out.println("Error: missing argument");
					return;
				}
				skipSec = Double.parseDouble(args[i]);
				break;
			case "-s":
				i++;
				if(i >= args.length) {
					System.out.println("Error: missing argument");
					return;
				}
				step = Integer.parseInt(args[i]);
				break;
			case "-m":
				i++;
				if(i >= args.length) {
					System.out.println("Error: missing argument");
					return;
				}
				minSec = Double.parseDouble(args[i]);
				break;
			case "-t":
				i++;
				if(i >= args.length) {
					System.out.println("Error: missing argument");
					return;
				}
				tail = Integer.parseInt(args[i]);
				break;
			case "-w":
				i++;
				if(i >= args.length) {
					System.out.println("Error: missing argument");
					return;
				}
				weight = Double.parseDouble(args[i]);
				break;
			case "-p":
				i++;
				if(i >= args.length) {
					System.out.println("Error: missing argument");
					return;
				}
				threadcnt = Integer.parseInt(args[i]);
				break;
			case "-o":
				i++;
				if(i >= args.length) {
					System.out.println("Error: missing argument");
					return;
				}
				outfileName = args[i];
				break;
			case "-k":
				i++;
				if(i >= args.length) {
					System.out.println("Error: missing argument");
					return;
				}
				key = MIDINames.getNoteNumber(args[i]);
				break;
			case "-n":
				i++;
				if(i >= args.length) {
					System.out.println("Error: missing argument");
					return;
				}
				key = Integer.parseInt(args[i]);
				break;
			case "-a":
				useSpectrum = true;
				break;
			default:
				System.out.println("Unknown option (arg " + (i + 1) + "): " + args[i]);
				return;
			}
		}

		if(wav == null) {
			System.out.println("Error: missing input file");
			return;
		}

		int sampleRate = wav.getSampleRate();
		int skip = (int) (sampleRate * skipSec);
		int minlen = (int) (sampleRate * minSec);

		int[][] samples = wav.getSamples();
		float[][] normalized = Normalizer.normalize(samples);

		if(samples.length == 1) {
			System.out.printf("%d channel, %d samples\n", samples.length, samples[0].length);
		} else {
			System.out.printf("%d channels, %d samples\n", samples.length, samples[0].length);
		}

		Loop[] loops;
		int loopcnt = 10;

		if(useSpectrum) {
			int fftSize = 2048;
			int windowSize = fftSize / 2;
			int stepSize = fftSize / 16;
			int tailLength = 1024; // roughly 2.5s at 48000Hz
			int minLength = (int) (minSec * sampleRate / stepSize);
			int loopskip = (int) (skipSec * sampleRate / stepSize);
			System.out.println("Using loop step size " + step + ", fft step size " + stepSize);
			System.out.print("Pass 1...");
			Loop[] structuralLoops = SpectraLoop.loop(normalized, loopskip, step, minLength, tailLength,
					loopcnt, fftSize, stepSize, FFT.HAMMING, windowSize, threadcnt);
			System.out.println(" done");

			if(structuralLoops == null || structuralLoops.length == 0) {
				System.out.println("failed to find loop");
				System.exit(1);
			}

			System.out.printf("Best loop from pass 1: %s-%s [%1.2f sec, error=%f]\n",
					structuralLoops[0].start, structuralLoops[0].end,
					structuralLoops[0].length / (double) sampleRate,
					structuralLoops[0].error);
			System.out.printf("Pass 2...");
			loops = LoopTuner.loop(normalized, stepSize, tail, loopcnt, structuralLoops, threadcnt);
			System.out.println(" done");
		} else {
			loops = Autoloop.loop(normalized, skip, step, minlen, tail, loopcnt, threadcnt);
		}

		if(loops == null) {
			System.out.println("failed to find loop");
			System.exit(1);
		}

		// filter and sort loops
		int nch = samples.length;
		int length = samples[0].length;

		final double _weight = weight;
		Comparator<Loop> comparator = (a, b) -> {
			double lenA = a.length / (double) length;
			double lenB = b.length / (double) length;

			double lenScoreA = 1.0 - lenA;
			double lenScoreB = 1.0 - lenB;

			double scoreA = a.error * _weight + lenScoreA * (1 - _weight);
			double scoreB = b.error * _weight + lenScoreB * (1 - _weight);

			return Double.compare(scoreA, scoreB);
		};

		final int _tail = tail;
		List<Loop> result = Stream.of(loops).filter(x -> x.error / _tail < ERROR_THRESHOLD).sorted(comparator)
				.collect(Collectors.toList());

		if(result.isEmpty()) {
			System.out.println("WARNING: SUBOPTIMAL LOOP!");
		}

		result = Stream.of(loops).sorted(comparator).collect(Collectors.toList());

		// print results
		result.stream().forEach(loop -> {
			double duration = (loop.end - loop.start) / (double) sampleRate;
			System.out.printf("loop: %d - %d [%1.2f sec, error=%s]\n", loop.start, loop.end,
					duration, loop.error / _tail);
		});

		if(!result.isEmpty() && outfileName != null) {
			int xfadelen = tail / 2;

			Loop loop = result.get(0);
			int[][] trimmed = new int[nch][loop.end + 1];
			int[][] xfade = new int[nch][xfadelen];
			for(int ch = 0; ch < nch; ch++) {
				System.arraycopy(samples[ch], 0, trimmed[ch], 0, trimmed[ch].length);
				System.arraycopy(samples[ch], loop.start - xfadelen, xfade[ch], 0, xfadelen);
			}

			CrossFader.crossfade(trimmed, xfade, loop.end - xfadelen);

			int note = key;
			if(note == -1) {
				System.out.print("Estimating root key...");
				double freq = PitchEstimator.estimate(normalized, sampleRate);
				note = (int) Math.round(Frequency.freqToMIDInote(freq));
				System.out.println(" done");
			}

			if(note < 0 || note > 127) {
				System.out.println("WARNING: invalid root key");
				note = 0;
			} else {
				System.out.println("root key: " + MIDINames.getNoteName(note) + " [" + note + "]");
			}

			wav.setSamples(trimmed);
			SampleChunk smpl = new SampleChunk();
			smpl.setMidiUnityNote(note);
			smpl.setMidiPitchFraction(0);
			smpl.setSamplePeriod(1_000_000_000 / sampleRate);
			smpl.addSampleLoop(new SampleLoop(0, SampleLoop.LOOP_FORWARD, loop.start, loop.end - 1, 0, 0));
			wav.set(smpl);

			try(OutputStream out = new FileOutputStream(outfileName)) {
				wav.write(out);
			}
		}
	}

	private static RiffWave loadWaveFile(String filename) throws IOException {
		try(InputStream in = new FileInputStream(filename)) {
			return Riff.read(in);
		}
	}
}
