/// <reference path="../pb_data/types.d.ts" />
/**
 * 允许 game_rooms.game_type = pac_maze（豆人迷宫在线对决/合作）。
 * 未部署此迁移时，客户端开房会收到：Invalid value pac_maze.
 */
migrate((app) => {
  const collection = app.findCollectionByNameOrId("game_rooms")
    || app.findCollectionByNameOrId("pbc_2955039625")

  const values = ["gomoku", "draw_guess", "dice_duel", "truth_relay", "pac_maze"]

  collection.fields.forEach((field) => {
    if (field.name === "game_type") {
      field.values = values
    }
  })

  return app.save(collection)
}, (app) => {
  const collection = app.findCollectionByNameOrId("game_rooms")
    || app.findCollectionByNameOrId("pbc_2955039625")

  const values = ["gomoku", "draw_guess", "dice_duel", "truth_relay"]

  collection.fields.forEach((field) => {
    if (field.name === "game_type") {
      field.values = values
    }
  })

  return app.save(collection)
})
