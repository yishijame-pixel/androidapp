/// <reference path="../pb_data/types.d.ts" />
migrate((app) => {
  const collection = new Collection({
    "createRule": "@request.auth.id = member_a || @request.auth.id = member_b",
    "deleteRule": "@request.auth.id = member_a || @request.auth.id = member_b",
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
        "cascadeDelete": false,
        "collectionId": "_pb_users_auth_",
        "help": "",
        "hidden": false,
        "id": "relation_member_a",
        "maxSelect": 1,
        "minSelect": 0,
        "name": "member_a",
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
        "id": "relation_member_b",
        "maxSelect": 1,
        "minSelect": 0,
        "name": "member_b",
        "presentable": false,
        "required": true,
        "system": false,
        "type": "relation"
      },
      {
        "autogeneratePattern": "",
        "help": "",
        "hidden": false,
        "id": "text_pair_key",
        "max": 64,
        "min": 3,
        "name": "pair_key",
        "pattern": "",
        "presentable": false,
        "primaryKey": false,
        "required": true,
        "system": false,
        "type": "text"
      },
      {
        "autogeneratePattern": "",
        "help": "",
        "hidden": false,
        "id": "text_last_preview",
        "max": 200,
        "min": 0,
        "name": "last_preview",
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
        "id": "number_last_message_at",
        "max": null,
        "min": null,
        "name": "last_message_at",
        "onlyInt": false,
        "presentable": false,
        "required": false,
        "system": false,
        "type": "number"
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
    "id": "pbc_728114816",
    "indexes": [
      "CREATE UNIQUE INDEX `idx_conv_pair_key` ON `conversations` (`pair_key`)"
    ],
    "listRule": "@request.auth.id = member_a || @request.auth.id = member_b",
    "name": "conversations",
    "system": false,
    "type": "base",
    "updateRule": "@request.auth.id = member_a || @request.auth.id = member_b",
    "viewRule": "@request.auth.id = member_a || @request.auth.id = member_b"
  });

  return app.save(collection);
}, (app) => {
  const collection = app.findCollectionByNameOrId("pbc_728114816");

  return app.delete(collection);
})
