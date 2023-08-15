# wavFile
Extension of the File class for WAV Files to provide additional functionality.

NOTE: This class is built with music production in mind. Because of this most functionality requires the WAV File to have an Acid / Acidization Chunk.
This data is usually exported by a DAW (Digital Audio Workstation).

## Public Methods
### Standard
- getDuration(): Returns the duration of audio in seconds
- getSampleRate(): Returns the sample rate of the audio
### ACID
- getTotalBeats(): Returns the number of beats in the the track (Rounded)
- getTempo(): Returns the Tempo (Beats per Minute) of the track
- getNumerator(): Returns the Time Signature Numerator
- getDenominator(): Returns the Time Signature Denominator
- getMarkers(): Returns a HashMap of Marker Names and Marker Times (ex. Chorus 1, 145.1)

## What is an ACID Chunk?
A standard WAV File is broken into multiple chunks of data. 
1. A RIFF Header (Defines the file as a WAVE file)
2. A fmt Chunk (Describes the data and sound format)
3. A data Chunk (The actual sound data)

Acidization is when a DAW (Digital Audio Workstation), used in music production, adds an additional ACID Chunk to the end of the file to provide data related to the music track and format.
This includes Root Notes, Number of Beats, Time Signature, Tempo and Markers (Available data is completely dependent on your DAW).

![acid](https://github.com/aabalke33/wavFile/assets/22086435/ff0207ac-bf4b-467c-bfc7-c10cddf15adf)

## Roadmap
- Fix instability with Markers, more than 8 cause errors
- Fix Tempo output when there is variance

## Sources and Further Research
https://forums.cockos.com/showthread.php?t=227118

http://soundfile.sapp.org/doc/WaveFormat/
