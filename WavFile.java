package wav;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Class to extend File class and add additional functionality for wav files
 */
public class WavFile extends File {
    /**
     * the file as a byte array
     */
    byte[] fileBytes;

    public WavFile(String pathname) throws IOException {
        super(pathname);
        this.fileBytes = convertToArray(super.getAbsoluteFile());
    }

    public WavFile(String parent, String child) throws IOException {
        super(parent, child);
        this.fileBytes = convertToArray(super.getAbsoluteFile());
    }

    public WavFile(File parent, String child) throws IOException {
        super(parent, child);
        this.fileBytes = convertToArray(super.getAbsoluteFile());
    }

    public WavFile(URI uri) throws IOException {
        super(uri);
        this.fileBytes = convertToArray(super.getAbsoluteFile());
    }

    /**
     * @return duration of wav file as a double
     */
    public double getDuration() {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(super.getAbsoluteFile());
            AudioFormat format = audioInputStream.getFormat();
            long frames = audioInputStream.getFrameLength();
            return (frames + 0.0) / format.getFrameRate();
        } catch (UnsupportedAudioFileException e) {
            System.out.println("Unsupported Audio File Exception");
        } catch (IOException e) {
            System.out.println("Could not read File");
        }
        return -1;
    }


    /**
     * @return the wav file sample rate
     */
    public int getSampleRate() {
        byte[] bytes = getChunk(fileBytes, "fmt");
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        return buffer.getInt(12);
    }

    /**
     * REQUIRES ACID CHUNK (ACIDIZATION in Digital audio Workstation)
     * @return the total number of beats
     */
    public int getTotalBeats() {
        byte[] bytes = getChunk(fileBytes, "acid");
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getInt(20);
    }

    /**
     * REQUIRES ACID CHUNK (ACIDIZATION in Digital audio Workstation)
     * @return the tempo
     */
    public float getTempo() {
        byte[] bytes = getChunk(fileBytes, "acid");
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getFloat(28);
    }
    /**
     * REQUIRES ACID CHUNK (ACIDIZATION in Digital audio Workstation)
     * @return the time signature numerator
     */
    public short getNumerator() {
        byte[] bytes = getChunk(fileBytes, "acid");
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getShort(24);
    }
    /**
     * REQUIRES ACID CHUNK (ACIDIZATION in Digital audio Workstation)
     * @return the time signature denominator
     */
    public short getDenominator() {
        byte[] bytes = getChunk(fileBytes, "acid");
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getShort(26);
    }
    /**
     * REQUIRES ACID CHUNK (ACIDIZATION in Digital audio Workstation)
     * @return a HashMap of Marker names and there time locations in seconds
     */
    public HashMap<String, Double> getMarkers() {

        ArrayList<String> markerLabels = getMarkerLabels(fileBytes);
        ArrayList<Double> markerTimes = getMarkerTimes(fileBytes);
        HashMap<String, Double> markers = new HashMap<>();

        for (int i = 0; i < markerLabels.size(); i++) {
            markers.put(markerLabels.get(i), markerTimes.get(i));
        }

        return markers;
    }

    /**
     * Method to convert file to byte array
     * @param file the wav file
     * @return byte array
     * @throws IOException
     */
    private byte[] convertToArray(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        fis.read(bytes);
        fis.close();
        return bytes;
    }

    /**
     * Method to get unique chunk as byte array
     * @param fileBytes entire file byte array
     * @param name name of chunk to be found
     * @return the chunk byte array
     */
    private byte[] getChunk(byte[] fileBytes, String name) {
        byte[] pattern = name.getBytes();
        int index = indexOf(fileBytes,pattern, 0);

        if (Arrays.equals(pattern, "fmt".getBytes())) {
            return Arrays.copyOfRange(fileBytes, index, index+20);
        }
        if (Arrays.equals(pattern, "acid".getBytes())) {
            return Arrays.copyOfRange(fileBytes, index, index+32);
        }
        return fileBytes;
    }
    /**
     * Method to get non-unique chunk as an arraylist of byte arrays
     * @param fileBytes entire file byte array
     * @param name name of chunks to be found
     * @return An arrayList of the chunk byte arrays
     */
    private ArrayList<byte[]> getChunks(byte[] fileBytes, String name, String exit) {
        ArrayList<byte[]> chunks = new ArrayList<>();
        ArrayList<Integer> chunkI = new ArrayList<>();
        byte[] pattern = name.getBytes();

        int index = 0;

        while (indexOf(fileBytes,pattern,index) > 0) {

            int newIndex = indexOf(fileBytes,pattern,index);
            chunkI.add(newIndex);
            index = newIndex + 1;
        }
        for (int i = 0; i < chunkI.size(); i++) {

            if ((i < chunkI.size()-1)) {
                chunks.add(Arrays.copyOfRange(fileBytes, chunkI.get(i), chunkI.get(i+1)));
            } else {
                chunks.add(Arrays.copyOfRange(fileBytes, chunkI.get(i), indexOf(fileBytes, exit.getBytes(), index)));
            }
        }
        return chunks;
    }

    /**
     * Method to get the index of byte array in a larger byte array
     * @param data Byte Array to search in
     * @param pattern Byte Array to search in
     * @param previousIndex Previous index, for searching multiple instances
     * @return index of pattern (first byte)
     */
    private int indexOf(byte[] data, byte[] pattern, int previousIndex) {
        if (data.length == 0) return -1;

        int[] failure = computeFailure(pattern);
        int j = 0;

        for (int i = previousIndex; i < data.length; i++) {
            while (j > 0 && pattern[j] != data[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == data[i]) { j++; }
            if (j == pattern.length) {
                return i - pattern.length + 1;
            }
        }
        return -1;
    }
    /**
     * Computes the failure function using a boot-strapping process,
     * where the pattern is matched against itself.
     */
    private int[] computeFailure(byte[] pattern) {
        int[] failure = new int[pattern.length];

        int j = 0;
        for (int i = 1; i < pattern.length; i++) {
            while (j > 0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }
        return failure;
    }

    /**
     * Method to trim byte array and remove trailing zeros
     * @param bytes byte array input
     * @return output byte array
     */
    private byte[] trimByteArray(byte[] bytes) {
        int i = bytes.length - 1;

        while (i >= 0 && bytes[i] == 0) {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }

    /**
     * Method to get Labels for Markers
     * @param fileBytes file byte array
     * @return ArrayList of String Labels
     */
    private ArrayList<String> getMarkerLabels(byte[] fileBytes) {

        ArrayList<String> labels = new ArrayList<>();
        ArrayList<byte[]> lablChunks = getChunks(fileBytes, "labl", "tlst");

        for (byte[] lablChunk : lablChunks) {
            ByteBuffer buffer = ByteBuffer.wrap(lablChunk).order(ByteOrder.LITTLE_ENDIAN);
            byte[] output = trimByteArray(Arrays.copyOfRange(buffer.array(), 12, buffer.array().length -1));
            String label = new String(output, StandardCharsets.UTF_8);
            labels.add(label);
        }
        return labels;
    }

    /**
     * Method to get times in seconds for Markers
     * @param fileBytes file byte array
     * @return ArrayList of Times in seconds
     */
    private ArrayList<Double> getMarkerTimes(byte[] fileBytes) {

        int rate = getSampleRate();
        ArrayList<Double> times = new ArrayList<>();
        ArrayList<byte[]> dataChunks = getChunks(fileBytes, "data\u0000", "LIST");

        for (byte[] dataChunk : dataChunks) {
            ByteBuffer buffer = ByteBuffer.wrap(dataChunk).order(ByteOrder.LITTLE_ENDIAN);
            int sampleTime = buffer.getInt(12);
            double time = ((double) sampleTime /(double) rate);
            times.add(time);
        }
        return times;
    }
}
