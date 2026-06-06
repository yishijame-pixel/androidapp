/// <reference path="../pb_data/types.d.ts" />
migrate((app) => {
  const existing = app.findCollectionByNameOrId("game_rooms")
  if (existing) {
    unmarshal({
      "listRule": "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')",
      "viewRule": "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')",
      "createRule": "@request.auth.id = host",
      "updateRule": "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')",
      "deleteRule": "@request.auth.id = host",
    }, existing)
    return app.save(existing)
  }

  const collection = new Collection({
    "createRule": "@request.auth.id = host",
    "deleteRule": "@request.auth.id = host",
    "listRule": "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')",
    "name": "game_rooms",
    "type": "base",
    "updateRule": "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')",
    "viewRule": "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')",
    "fields": [
      {
        "autogeneratePattern": "[a-z0-9]{15}",
        "hidden": false,
        "id": "text3208210256",
        "max": 15,
        "min": 15,
        "name": "id",
        "pattern": "^[a-z0-9]+$",
        "presentable": false,
        "primaryKey": true,
        "required": true,
        "system": true,
        "type": "text"
      },
      {
        "hidden": false,
        "id": "select_game_type",
        "maxSelect": 1,
        "name": "game_type",
        "presentable": false,
        "required": true,
        "system": false,
        "type": "select",
        "values": ["gomoku", "draw_guess", "dice_duel", "truth_relay"]
      },
      {
        "hidden": false,
        "id": "select_invite_mode",
        "maxSelect": 1,
        "name": "invite_mode",
        "presentable": false,
        "required": true,
        "system": false,
        "type": "select",
        "values": ["direct", "open"]
      },
      {
        "autogeneratePattern": "",
        "hidden": false,
        "id": "text_room_code",
        "max": 6,
        "min": 0,
        "name": "room_code",
        "pattern": "",
        "presentable": false,
        "primaryKey": false,
        "required": false,
        "system": false,
        "type": "text"
      },
      {
        "cascadeDelete": false,
        "collectionId": "_pb_users_auth_",
        "hidden": false,
        "id": "relation_host",
        "maxSelect": 1,
        "minSelect": 0,
        "name": "host",
        "presentable": false,
        "required": true,
        "system": false,
        "type": "relation"
      },
      {
        "cascadeDelete": false,
        "collectionId": "_pb_users_auth_",
        "hidden": false,
        "id": "relation_guest",
        "maxSelect": 1,
        "minSelect": 0,
        "name": "guest",
        "presentable": false,
        "required": false,
        "system": false,
        "type": "relation"
      },
      {
        "hidden": false,
        "id": "select_status",
        "maxSelect": 1,
        "name": "status",
        "presentable": false,
        "required": true,
        "system": false,
        "type": "select",
        "values": [
          "waiting",
          "accepted",
          "playing",
          "finished",
          "cancelled",
          "expired",
          "abandoned"
        ]
      },
      {
        "hidden": false,
        "id": "bool_host_ready",
        "name": "host_ready",
        "presentable": false,
        "required": false,
        "system": false,
        "type": "bool"
      },
      {
        "hidden": false,
        "id": "bool_guest_ready",
        "name": "guest_ready",
        "presentable": false,
        "required": false,
        "system": false,
        "type": "bool"
      },
      {
        "cascadeDelete": false,
        "collectionId": "_pb_users_auth_",
        "hidden": false,
        "id": "relation_current_turn",
        "maxSelect": 1,
        "minSelect": 0,
        "name": "current_turn",
        "presentable": false,
        "required": false,
        "system": false,
        "type": "relation"
      },
      {
        "cascadeDelete": false,
        "collectionId": "_pb_users_auth_",
        "hidden": false,
        "id": "relation_winner",
        "maxSelect": 1,
        "minSelect": 0,
        "name": "winner",
        "presentable": false,
        "required": false,
        "system": false,
        "type": "relation"
      },
      {
        "hidden": false,
        "id": "json_game_state",
        "maxSize": 2000000,
        "name": "game_state",
        "presentable": false,
        "required": false,
        "system": false,
        "type": "json"
      },
      {
        "autogeneratePattern": "",
        "hidden": false,
        "id": "text_invite_message",
        "max": 50,
        "min": 0,
        "name": "invite_message",
        "pattern": "",
        "presentable": false,
        "primaryKey": false,
        "required": false,
        "system": false,
        "type": "text"
      },
      {
        "hidden": false,
        "id": "date_expires_at",
        "max": "",
        "min": "",
        "name": "expires_at",
        "presentable": false,
        "required": false,
        "system": false,
        "type": "date"
      },
      {
        "hidden": false,
        "id": "date_finished_at",
        "max": "",
        "min": "",
        "name": "finished_at",
        "presentable": false,
        "required": false,
        "system": false,
        "type": "date"
      },
      {
        "hidden": false,
        "id": "autodate2990389176",
        "name": "created",
        "onCreate": true,
        "onUpdate": false,
        "presentable": false,
        "system": false,
        "type": "autodate"
      },
      {
        "hidden": false,
        "id": "autodate3332085495",
        "name": "updated",
        "onCreate": true,
        "onUpdate": true,
        "presentable": false,
        "system": false,
        "type": "autodate"
      }
    ],
    "indexes": [
      "CREATE INDEX `idx_game_room_host_status` ON `game_rooms` (`host`, `status`)",
      "CREATE INDEX `idx_game_room_guest_status` ON `game_rooms` (`guest`, `status`)",
      "CREATE INDEX `idx_game_room_code` ON `game_rooms` (`room_code`)"
    ]
  });

  return app.save(collection);
}, (app) => {
  const collection = app.findCollectionByNameOrId("game_rooms");
  return app.delete(collection);
});
