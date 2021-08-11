package blockchain.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DifficultyAdjusterTest {
    private DifficultyAdjuster difficultyAdjuster;

    private static Block dummyBlock0;
    private static Block dummyBlock1;
    private static Block block2;
    private static Block block3;
    private static Block block4;
    private static Block block5;
    private static Block block6;

    @BeforeEach
    void setUp() {
        difficultyAdjuster = new DifficultyAdjuster();
    }

    @BeforeAll
    static void beforeAll() {
        dummyBlock0 = Block.newBuilder().setId(0).build();
        dummyBlock1 = Block.newBuilder().setId(1).setTimestamp(0).build();
        block2 = Block.newBuilder().setId(2).setTimestamp(1000).build();
        block3 = Block.newBuilder().setId(3).setTimestamp(5000).build();
        block4 = Block.newBuilder().setId(4).setTimestamp(8000).build();
        block5 = Block.newBuilder().setId(5).setTimestamp(40000).build();
        block6 = Block.newBuilder().setId(6).setTimestamp(100000).build();
    }

    @Test
    void adjustDifficultyTest() {
        List<Block> expectedDecrease = List.of(dummyBlock0, dummyBlock1, block2, block3, block4, block5, block6);
        difficultyAdjuster.adjustDifficulty(expectedDecrease);
        int difficultyValue = difficultyAdjuster.getDifficultyValue();
        assertEquals(4, difficultyValue);

        /* Resetting difficulty counter to 5 */
        difficultyAdjuster = new DifficultyAdjuster();
        List<Block> expectedIncrease = List.of(dummyBlock0, dummyBlock1, block2, block3);
        difficultyAdjuster.adjustDifficulty(expectedIncrease);
        difficultyValue = difficultyAdjuster.getDifficultyValue();
        assertEquals(6, difficultyValue);
    }

    @Test
    void calculateCurrentDifficultyTest() {
        List<Block> blockListIncreaseExpected = List.of(dummyBlock0, dummyBlock1, block2, block3, block4);
        List<Block> blockListTheSameExpected = List.of(dummyBlock0, dummyBlock1, block2, block3, block4, block5);
        List<Block> blockListDecreasedExpected = List.of(dummyBlock0, dummyBlock1, block2, block3, block4, block5, block6);
        int expectedIncrease = difficultyAdjuster.calculateCurrentDifficulty(blockListIncreaseExpected);
        assertEquals(6, expectedIncrease);

        int expectedTheSame = difficultyAdjuster.calculateCurrentDifficulty(blockListTheSameExpected);
        assertEquals(6, expectedTheSame);

        int expectedDecrease = difficultyAdjuster.calculateCurrentDifficulty(blockListDecreasedExpected);
        assertEquals(5, expectedDecrease);
    }

    @Test
    void calcAverageCreationTimeTest() {
        List<Block> blockList = List.of(dummyBlock0, dummyBlock1, block2, block3, block4);
        long averageCreationTime = difficultyAdjuster.calcAverageCreationTime(block4, blockList);
        assertEquals(8000 / 3, averageCreationTime);

        List<Block> blockListExtended = List.of(dummyBlock0, dummyBlock1, block2, block3, block4, block5);
        averageCreationTime = difficultyAdjuster.calcAverageCreationTime(block4, blockListExtended);
        assertEquals(8000 / 3, averageCreationTime);

        averageCreationTime = difficultyAdjuster.calcAverageCreationTime(block5, blockListExtended);
        assertEquals(39000 / 3, averageCreationTime);
    }
}