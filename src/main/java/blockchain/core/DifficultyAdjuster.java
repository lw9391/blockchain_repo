package blockchain.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DifficultyAdjuster {
    private volatile int difficultyValue = 5;

    public static final int DIFFICULTY_TARGET = 15;
    public static final int DIFFICULTY_TOLERANCE = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(DifficultyAdjuster.class);

    /* After appending a new block, this method adjust blockchain difficulty if it is necessary. */
    public void adjustDifficulty(List<Block> blocks) {
        Block newlyAppended = blocks.get(blocks.size() - 1);
        if (newlyAppended.getId() % 3 != 0) {
            return;
        }
        long averageTime = calcAverageCreationTime(newlyAppended, blocks) / 1000;
        LOGGER.info("Average time creation of 3 last block is " + averageTime + " seconds");

        synchronized (this) {
            int difficultyIndex = difficultyCheck(averageTime, difficultyValue);
            if (difficultyIndex == 1) {
                difficultyValue++;
                LOGGER.info("N was increased to " + difficultyValue);
            } else if (difficultyIndex == -1) {
                difficultyValue--;
                LOGGER.info("N was decreased by 1");
            } else {
                LOGGER.info("N stays the same.");
            }
        }
    }

    /* Recalculate current difficulty from the beginning. Useful in case of loading blockchain from file/network. */
    public int calculateCurrentDifficulty(List<Block> blocks) {
        ArrayList<Block> blocksArray = new ArrayList<>(blocks);
        int difficulty = 5;
        for (int i = 3; i < blocksArray.size(); i+=3) {
            Block current = blocksArray.get(i);
            long time = calcAverageCreationTime(current, blocksArray) / 1000;
            int step = difficultyCheck(time, difficulty);
            difficulty += step;
        }
        synchronized (this) {
            difficultyValue = difficulty;
        }
        return difficulty;
    }

    private int difficultyCheck(long time, int currentDiff) {
        if (time < (DIFFICULTY_TARGET - DIFFICULTY_TOLERANCE) && currentDiff < 6) {
            return 1;
        } else if (time > (DIFFICULTY_TARGET + DIFFICULTY_TOLERANCE) && currentDiff > 2) {
            return -1;
        } else return 0;
    }

    public long calcAverageCreationTime(Block block, List<Block> blockList) {
        int start = block.getId();
        int end = Math.max(start - 3, 1);
        long summedTime = 0;
        for (int i = start; i > end; i--) {
            Block later = blockList.get(i);
            Block earlier = blockList.get(i - 1);
            long compared = later.getTimestamp() - earlier.getTimestamp();
            summedTime += compared;
        }
        return summedTime / (start - end);
    }

    public synchronized int getDifficultyValue() {
        return difficultyValue;
    }
}