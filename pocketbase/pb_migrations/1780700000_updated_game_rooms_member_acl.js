/// <reference path="../pb_data/types.d.ts" />
/**
 * 允许 game_state.members 中的宾客读写房间（接受邀请后会清空 legacy guest 字段）。
 * 否则宾客 PATCH/GET 会 404，客户端显示 "The requested resource wasn't found."
 */
migrate((app) => {
  const collection = app.findCollectionByNameOrId("pbc_2955039625")

  const memberAccessRule =
    "@request.auth.id = host || " +
    "@request.auth.id = guest || " +
    "game_state ~ @request.auth.id || " +
    "(status = 'waiting' && invite_mode = 'open')"

  unmarshal(
    {
      listRule: memberAccessRule,
      viewRule: memberAccessRule,
      updateRule: memberAccessRule,
      createRule: "@request.auth.id = host && @request.auth.id != ''",
      deleteRule: "@request.auth.id = host",
    },
    collection,
  )

  return app.save(collection)
}, (app) => {
  const collection = app.findCollectionByNameOrId("pbc_2955039625")

  const legacyRule =
    "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')"

  unmarshal(
    {
      listRule: legacyRule,
      viewRule: legacyRule,
      updateRule: legacyRule,
    },
    collection,
  )

  return app.save(collection)
})
