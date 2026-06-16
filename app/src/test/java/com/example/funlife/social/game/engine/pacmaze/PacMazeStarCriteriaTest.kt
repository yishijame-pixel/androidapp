package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Test

class PacMazeStarCriteriaTest {

    private val criteria = PacMazeStarCriteria(
        twoStarMinScore = 2000,
        threeStarMinScore = 3000,
        threeStarMaxSeconds = 120,
        threeStarNoDeath = true,
        threeStarRequiredTags = setOf("L14-W", "L14-E", "L14-CORE"),
    )

    @Test
    fun threeStars_requiresAllCheckpointTags() {
        assertEquals(2, PacMazeStarEvaluator.evaluate(criteria, score = 3500, elapsedSeconds = 60, deaths = 0, emptySet()))
        assertEquals(
            3,
            PacMazeStarEvaluator.evaluate(
                criteria,
                score = 3500,
                elapsedSeconds = 60,
                deaths = 0,
                visitedCheckpointTags = setOf("L14-W", "L14-E", "L14-CORE"),
            ),
        )
    }

    @Test
    fun checkpointVisits_recordNonLinkTags() {
        val json = """
            {
              "id": 14,
              "name": "visit-test",
              "width": 7,
              "height": 7,
              "grid": [
                "#######",
                "#.....#",
                "#.....#",
                "#.....#",
                "#.....#",
                "#.....#",
                "#######"
              ],
              "spawn": { "pac": [3, 5], "ghosts": [[3, 1]] },
              "markers": [
                { "type": "checkpoint", "x": 3, "y": 3, "label": "核", "tag": "L14-CORE" }
              ]
            }
        """.trimIndent()
        val level = PacMazeMapLoader.parseLevelJson(json)
        var world = PacMazeMapLoader.buildInitialWorld(level, json, seed = 1L)
        world = world.copy(
            entities = world.entities.map { e ->
                if (e.role == "pac") e.copy(x = 3.5f, y = 3.5f) else e
            },
        )
        world = PacMazeCheckpointVisits.apply(world, level)
        assertEquals(setOf("L14-CORE"), world.visitedCheckpointTags)
    }
}
