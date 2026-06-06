/// <reference path="../pb_data/types.d.ts" />
/**
 * 修复 game_rooms ACL：
 * - 恢复 createRule（调试时被误改为 @request.auth.id != ''）
 * - 用 game_state ~ id 替代无效的 member_ids ?= 语法（PB 0.39 会导致写入失败）
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

  unmarshal(
    {
      listRule:
        "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')",
      viewRule:
        "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')",
      updateRule:
        "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')",
      createRule: "@request.auth.id != ''",
      deleteRule: "@request.auth.id = host",
    },
    collection,
  )

  return app.save(collection)
})
