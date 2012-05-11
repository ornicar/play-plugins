package com.typesafe.plugin

import play.api._
import org.sedis._
import redis.clients.jedis._
import play.api.cache._
import java.util._
import java.io._
import biz.source_code.base64Coder._

/**
 * provides a redis client and a CachePlugin implementation
 * the cache implementation can deal with the following data types:
 * - classes implement Serializable
 * - String, Int, Boolean and long
 */
class RedisPlugin(app: Application) extends CachePlugin {

  private lazy val host = app.configuration.getString("redis.host").getOrElse("localhost")
  private lazy val port = app.configuration.getInt("redis.port").getOrElse(6379)
  private lazy val timeout = app.configuration.getInt("redis.timeout").getOrElse(2000)
  private lazy val password = app.configuration.getString("redis.password").getOrElse(null)

  /**
   * provides access to the underlying jedis Pool
   */
  lazy val jedisPool = new JedisPool(new JedisPoolConfig(), host, port, timeout, password)

  /**
   * provides access to the sedis Pool
   */
  lazy val sedisPool = new Pool(jedisPool)

  override def onStart() {
    sedisPool
  }

  override def onStop() {
    jedisPool.destroy()
  }

  override lazy val enabled = {
    !app.configuration.getString("redisplugin").filter(_ == "disabled").isDefined
  }

  /**
   * cacheAPI implementation
   * can serialize, deserialize to/from redis
   * value needs be Serializable (a few primitive types are also supported: String, Int, Long, Boolean)
   */
  lazy val api = new CacheAPI {

    def set(key: String, value: Any, expiration: Int) {
      sedisPool.withJedisClient { client ⇒
        client.set(key, value.toString)
        if (expiration != 0) client.expire(key, expiration)
      }
    }

    def get(key: String): Option[Any] =
      sedisPool.withJedisClient { client ⇒
        Dress up client get key
      }
  }
}
