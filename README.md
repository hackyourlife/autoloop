Autoloop
========

A simple autolooper based on mean squared errors.

Compiling
---------

Requirements:

- Java 8+

```
./compile
```

Usage
-----

```
java -jar autoloop.jar OPTIONS...
```

- `-f file.wav`: Input file in wav format (8/16/24bit integer or 32bit float)
- `-i 0.0`: Skip that many seconds in the beginning [default: 0.0]
- `-s 4000`: Step through the audio file in n sample steps [default: 4000]
- `-m 1.0`: Minimum loop length of n seconds [default: 1.0]
- `-t 1000`: Number of samples to check after loop end [default: 1000]
- `-w 1.0`: Weighting factor to trade loop length vs loop quality [default: 1.0, meaning only quality is relevant]
- `-p 2`: Number of concurrent threads for analysis [default: 2]
- `-o out.wav`: Output file (trimmed + embedded loop points)
- `-k c#4`: Root key (name) to embed in the output file [conflicts with `-n`; automatically estimated if no key is given]
- `-n 60`: Root key (MIDI number) to embed in the output file [conflicts with `-k`; automatically estimated if no key is given]
- `-a`: Use advanced analysis to detect long repeating patterns

In normal mode, `-t` many samples are checked for equality after the loop
point. This typically works fine for simple sounds, but it fails to capture
longer structures like LFO modulations on pitch/filter/... That is where
advanced mode comes in: it uses a different analysis approach to find long
repeating structures in the sample and then tunes the loop point to be near the
long structure. This works for wave sequences, LFO modulated pitch/filter/...,
but it produces suboptimal results for simple waveforms.

Example
-------

Assume a sample was recorded at 48000kHz. You know for sure that the first 3
seconds will never be a good loop start. And you think a loop should be at
least two seconds long. Your system has 12 CPU cores, so you want to use all of
them. You want to get a new wav file called `looped.wav` with loop points
embedded.

```
java -jar autoloop.jar -i recording.wav -i 3 -m 2 -p 12 -o looped.wav
```
