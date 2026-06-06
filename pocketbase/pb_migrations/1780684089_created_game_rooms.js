/// <reference path="../pb_data/types.d.ts" />
migrate((app) => {
  const collection = new Collection({
    "createRule": "@request.auth.id = host",
    "deleteRule": "@request.auth.id = host",
    "fields": [
      {
        "autogeneratePattern": "[a-z0-9]{15}",
        "help": "",
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
        "help": "",
        "hidden": false,
        "id": "select_game_type",
        "maxSelect": 1,
        "name": "game_type",
        "presentable": false,
        "required": true,
        "system": false,
        "type": "select",
        "values": [
          "gomoku",
          "draw_guess",
          "dice_duel",
          "truth_relay"
        ]
      },
      {
        "help": "",
        "hidden": false,
        "id": "select_invite_mode",
        "maxSelect": 1,
        "name": "invite_mode",
        "presentable": false,
        "required": true,
        "system": false,
        "type": "select",
        "values": [
          "direct",
          "open"
        ]
      },
      {
        "autogeneratePattern": "",
        "help": "",
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
        "help": "",
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
        "help": "",
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
        "help": "",
        "hidden": false,
        "id": "select_status",
        "maxSelect": 1,
        "name": "status",
        "presentable": false,
        "required": true,
        "system": false,
        "type": "select",
        "values": [
          "invited",
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
        "help": "",
        "hidden": false,
        "id": "bool_host_ready",
        "name": "host_ready",
        "presentable": false,
        "required": false,
        "system": false,
        "type": "bool"
      },
      {
        "help": "",
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
        "help": "",
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
        "help": "",
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
        "help": "",
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
        "help": "",
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
        "help": "",
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
        "help": "",
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
    "id": "pbc_2955039625",
    "indexes": [
      "CREATE INDEX `idx_game_room_host_status` ON `game_rooms` (`host`, `status`)",
      "CREATE INDEX `idx_game_room_guest_status` ON `game_rooms` (`guest`, `status`)",
      "CREATE INDEX `idx_game_room_code` ON `game_rooms` (`room_code`)"
    ],
    "listRule": "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')",
    "name": "game_rooms",
    "system": false,
    "type": "base",
    "updateRule": "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')",
    "viewRule": "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')"
  });

  return app.save(collection);
}, (app) => {
  const collection = app.findCollectionByNameOrId("pbc_2955039625");

  return app.delete(collection);
})
