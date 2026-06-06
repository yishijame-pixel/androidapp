/// <reference path="../pb_data/types.d.ts" />
migrate((app) => {
  const collection = app.findCollectionByNameOrId("pbc_2955039625")

  // update collection data
  unmarshal({
    "indexes": [
      "CREATE INDEX `idx_game_room_host_status` ON `game_rooms` (`host`, `status`)",
      "CREATE INDEX `idx_game_room_guest_status` ON `game_rooms` (`guest`, `status`)"
    ]
  }, collection)

  return app.save(collection)
}, (app) => {
  const collection = app.findCollectionByNameOrId("pbc_2955039625")

  // update collection data
  unmarshal({
    "indexes": [
      "CREATE INDEX `idx_game_room_host_status` ON `game_rooms` (`host`, `status`)",
      "CREATE INDEX `idx_game_room_guest_status` ON `game_rooms` (`guest`, `status`)",
      "CREATE UNIQUE INDEX `idx_game_room_code` ON `game_rooms` (`room_code`) WHERE `room_code` != ''"
    ]
  }, collection)

  return app.save(collection)
})
