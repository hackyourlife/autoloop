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
java -jar autoloop.jar recording.wav skip step min-size tail [threads]
```

- `recording.wav`: the recorded file in RIFF WAVE format. Supported formats:
  8/16/24bit PCM
- `skip`: number of samples to skip at the beginning
- `step`: step size for loop start search (in samples)
- `min-size`: minimum length (in samples) for a loop
- `tail`: tail length, used for loop quality check (in samples)
- `threads`: number of threads to use for parallel execution; this can give
  significant speedups

Example
-------

Assume a sample was recorded at 48000kHz. You know for sure that the first 3
seconds will never be a good loop start. And you think a loop should be at
least two seconds long. Your system has 12 CPU cores, so you want to use all of
them.

```
java -jar autoloop.jar recording.wav 144000 8000 96000 1000 12
```
