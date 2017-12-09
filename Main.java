import jm.music.data.*;
import jm.util.*;

import java.util.ArrayList;
import java.util.Random;

import static jm.constants.Durations.EIGHTH_NOTE;
import static jm.constants.Durations.QUARTER_NOTE;
import static jm.constants.ProgramChanges.*;


public class Main {

    private static final int CHORDS_UPPER_BOUND = 72;
    private static final int CHORDS_LOWER_BOUND = 48;

    private static final int NOTES_UPPER_BOUND = 96;
    private static final int NOTES_LOWER_BOUND = 72;

    private static final int AMOUNT_OF_PARTICLES = 5000; // same for both PSO
    private static final int ITERATIONS = 10000; // same for both PSO

    private static final double END_COEFFICIENT_FOR_CHORDS = 0.975;
    private static final double END_COEFFICIENT_FOR_NOTES = 0.905;


    private static final int[] cMajor = {48, 50, 52, 53, 55, 57, 59, 60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81,
            83, 84, 86, 88, 89, 91, 93, 95, 96};


    public static void main(String[] args){

        long startTime = System.nanoTime();

        Score score = new Score("My Melody");
        Part chord = new Part("Accompaniment", PIANO, 0);
        Part note = new Part("Notes", PIANO, 0);

        // array lists which are store chords and notes as integers in midi format
        ArrayList<ArrayList<Integer>> chords = pso1();
        ArrayList<Integer> notes = pso2(chords);

        // add phrases to parts
        chord.addCPhrase(formChords(chords));
        note.addPhrase(formNotes(notes));

        // finally add 2 parts to midi file and set tempo
        score.addPart(chord);
        score.addPart(note);
        score.setTempo(120);
        Write.midi(score, "melody.mid");

        // write spent time to console
        System.out.print("Spent time: ");
        System.out.print((System.nanoTime() - startTime) / 1000_000);
        System.out.print(" milliseconds");
    }

    /**
     * PSO #1
     * @return integer array of array of 3 integers that represents notes for chords
     */
    private static ArrayList<ArrayList<Integer>> pso1(){

        double gBest = 0;
        int[] gBestValues = new int[16];
        int[][] particles = new int[AMOUNT_OF_PARTICLES][16]; // declaration of particles
        double[][] velocities = new double[AMOUNT_OF_PARTICLES][16];
        double[] lBest = new double[AMOUNT_OF_PARTICLES];
        int[][] lBestValues = new int[AMOUNT_OF_PARTICLES][16];

        // initialize particles with random values
        Random random = new Random(System.currentTimeMillis());
        for(int i = 0; i < AMOUNT_OF_PARTICLES; i++){
            for(int j = 0; j < 16; j++){
                particles[i][j] = random.nextInt(24) + 48;
            }
            for(int j = 0; j < 16; j++){
                velocities[i][j] = random.nextDouble();
            }
            lBest[i] = fitnessFunctionForChords(particles[i]);
            System.arraycopy(particles[i], 0, lBestValues[i], 0, 16);

            // update global best
            if(i == 0 || gBest < lBest[i]) {
                gBest = lBest[i];
                System.arraycopy(lBestValues[i], 0, gBestValues, 0, 16);
            }
        }

        // main body of PSO. End condition is number of some iterations or some coefficient that our PSO achieved.
        for(int i = 0; i < ITERATIONS; i++){

            if(gBest >= END_COEFFICIENT_FOR_CHORDS) {
                break;
            }

            for(int j = 0; j < AMOUNT_OF_PARTICLES; j++){

                // update our particles with new values
                for(int y = 0; y < 16; y++){
                    velocities[j][y] = 0.5 * velocities[j][y] + 2 * random.nextDouble() * (lBestValues[j][y] - particles[j][y])
                            + 2 * random.nextDouble() * (gBestValues[y] - particles[j][y]);
                    particles[j][y] = (int) ((double) particles[j][y] + velocities[j][y]);
                }

                // calculate fitness function for new particles and update local bests
                double newVal = fitnessFunctionForChords(particles[j]);
                if(newVal > lBest[j]) {
                    lBest[j] = newVal;
                    System.arraycopy(particles[j], 0, lBestValues[j], 0, 16);
                }

                // update global best
                if(gBest < lBest[j]) {
                    gBest = lBest[j];
                    System.arraycopy(lBestValues[j], 0, gBestValues, 0, 16);
                }
            }
        }

        // finally pack it on ArrayList
        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        for(int i = 0; i < 16; i++){
            ArrayList<Integer> arrayList = new ArrayList<>();
            if(gBestValues[i] < 0 || gBestValues[i] > 127){
                arrayList.add(60);
                arrayList.add(64);
                arrayList.add(67);
                System.out.println(gBestValues[i]);
            } else{
                arrayList.add(gBestValues[i]);
                arrayList.add(gBestValues[i] + 4);
                arrayList.add(gBestValues[i] + 7);
            }

            result.add(arrayList);
        }
        return result;
    }

    /**
     * Method to construct chords from integers
     * @param chords - array that contains array with 3 integers, which are notes for chord
     * @return CPhrase object, which then will written to midi file
     */
    private static CPhrase formChords(ArrayList<ArrayList<Integer>> chords){
        CPhrase cPhrase = new CPhrase();
        for(int i = 0; i < 16; i++){
            Note[] notes = new Note[3];
            notes[0] = new Note(chords.get(i).get(0), QUARTER_NOTE);
            notes[1] = new Note(chords.get(i).get(1), QUARTER_NOTE);
            notes[2] = new Note(chords.get(i).get(2), QUARTER_NOTE);
            cPhrase.addChord(notes);
        }
        return cPhrase;
    }

    /**
     * PSO #2
     * @param chords - 2-d array of notes which are represent chords
     * @return ArrayList of integers which are represent notes
     */
    private static ArrayList<Integer> pso2(ArrayList<ArrayList<Integer>> chords){
        Random r = new Random();

        double gBest = 0;
        int[] gBestValues = new int[32];
        int[][] particles = new int[AMOUNT_OF_PARTICLES][32]; // declaration of particles
        double[][] velocities = new double[AMOUNT_OF_PARTICLES][32];
        double[] lBest = new double[AMOUNT_OF_PARTICLES];
        int[][] lBestValues = new int[AMOUNT_OF_PARTICLES][32];

        // initialize particles with random values
        Random random = new Random(System.currentTimeMillis());
        for(int i = 0; i < AMOUNT_OF_PARTICLES; i++){
            for(int j = 0; j < 32; j++){
                particles[i][j] = r.nextInt(24) + 72;
            }
            for(int j = 0; j < 32; j++){
                velocities[i][j] = random.nextDouble();
            }
            lBest[i] = fitnessFunctionForNotes(particles[i], chords);
            System.arraycopy(particles[i], 0, lBestValues[i], 0, 32);

            // update global best
            if(i == 0 || gBest < lBest[i]) {
                gBest = lBest[i];
                System.arraycopy(lBestValues[i], 0, gBestValues, 0, 32);
            }
        }

        // main body of PSO. End condition is number of some iterations or some coefficient that our PSO achieved.
        for(int i = 0; i < ITERATIONS; i++){

            if(gBest >= END_COEFFICIENT_FOR_NOTES) {
                break;
            }

            for(int j = 0; j < AMOUNT_OF_PARTICLES; j++){

                // update our particles with new values
                for(int y = 0; y < 32; y++){
                    velocities[j][y] = 0.5 * velocities[j][y] + 2 * random.nextDouble() * (lBestValues[j][y] - particles[j][y])
                            + 2 * random.nextDouble() * (gBestValues[y] - particles[j][y]);
                    particles[j][y] = (int) ((double) particles[j][y] + velocities[j][y]);
                }

                // calculate fitness function for new particles and update local bests
                double newVal = fitnessFunctionForNotes(particles[j], chords);
                if(newVal > lBest[j]) {
                    lBest[j] = newVal;
                    System.arraycopy(particles[j], 0, lBestValues[j], 0, 32);
                }

                // update global best
                if(gBest < lBest[j]) {
                    gBest = lBest[j];
                    System.arraycopy(lBestValues[j], 0, gBestValues, 0, 32);
                }
            }
        }

        // finally pack it on ArrayList
        ArrayList<Integer> result = new ArrayList<>();
        for(int i = 0; i < 32; i++){
            result.add(gBestValues[i]);
        }
        return result;
    }

    /**
     * Method for translating array of notes to Phrase
     * @param notes - array of integers
     * @return Phrase with 32 notes, durations of which are eighth notes
     */
    private static Phrase formNotes(ArrayList<Integer> notes){
        Phrase phrase = new Phrase();
        for(int i = 0; i < 32; i++){
            Note note = new Note(notes.get(i), EIGHTH_NOTE);
            phrase.add(note);
        }
        return phrase;
    }

    /**
     * Method that returns some double value which says how good is this sequence of notes
     * @param startNotes - array of starting notes for each chords
     * @return double value which says how good is this sequence of notes
     */
    private static double fitnessFunctionForChords(int[] startNotes){
        double answer = 0;
        int size = startNotes.length;

        // check that there is no repeating of same note more than 5 times
        if(!isAllEqual(startNotes))
            answer += 1;

        // check that notes are in right diapason
        for (int startNote1 : startNotes) {
            if (startNote1 < CHORDS_UPPER_BOUND && startNote1 >= CHORDS_LOWER_BOUND)
                answer += 0.0625;
        }

        // check that difference between 2 neighboring note less than 12
        for(int i = 1; i < size; i++){
            if(Math.abs(startNotes[i] - startNotes[i - 1]) < 12)
                answer += 0.066667;
        }

        // first 5 chords are increase
        for(int i = 1; i < 4; i++){
            if(startNotes[i] > startNotes[i - 1])
                answer += 0.125;
        }

        // last 5 chords are decrease
        for(int i = 12; i < 16; i++){
            if(startNotes[i] < startNotes[i - 1])
                answer += 0.125;
        }

        // check that notes in C Major tonality
        for (int startNote : startNotes) {
            for (int aCMajor : cMajor) {
                if (startNote == aCMajor) {
                    answer += 0.0625;
                    break;
                }
            }
        }

        answer /= 5;

        return answer;
    }

    private static double fitnessFunctionForNotes(int[] startNotes, ArrayList<ArrayList<Integer>> chords){
        double answer = 0;
        int size = startNotes.length;

        // check that there is no repeating of same note more than 5 times
        if(!isAllEqual(startNotes))
            answer += 1;

        // check that notes are in right diapason
        for (int startNote : startNotes) {
            if (startNote <= NOTES_UPPER_BOUND && startNote >= NOTES_LOWER_BOUND)
                answer += 0.03125;
        }

        // check that we are to one or more octaves higher than first note in chord
        for(int i = 0; i < size; i++){
            int c1 = chords.get(i / 2).get(0);
            if(Math.abs(c1 - startNotes[i]) % 12 == 0)
                answer += 0.03125;
        }

        // check that difference between 2 neighboring notes less than 12
        for(int i = 0; i < size - 1; i++){
            if(Math.abs(startNotes[i] - startNotes[i + 1]) <= 12)
                answer += 0.0322580645;
        }


        // first 10 notes are increase
        for(int i = 1; i < 9; i++){
            if(startNotes[i] > startNotes[i - 1])
                answer += 0.05;
        }


        // last 10 notes are decrease
        for(int i = 24; i < 32; i++){
            if(startNotes[i] < startNotes[i - 1])
                answer += 0.05;
        }

        // check that notes in C Major tonality
        for (int startNote : startNotes) {
            for (int aCMajor : cMajor) {
                if (startNote == aCMajor) {
                    answer += 0.03125;
                    break;
                }
            }
        }

        answer /= 6;

        return answer;
    }

    /**
     * Method that checks whether array contains sequence of 5 or more same integers
     * @param a - is array of integers
     * @return whether array contains sequence of 5 or more same integers
     */
    private static boolean isAllEqual(int[] a){
        int t = a[0];
        int repeatings = 1;
        for(int i = 1; i < a.length; i++){
            if(t == a[i])
                repeatings++;
            else
                repeatings = 1;
            if(repeatings == 5)
                return true;
        }
        return false;
    }
}
