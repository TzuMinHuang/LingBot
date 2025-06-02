package idv.hzm.app.common.util;

import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CodeGeneratorUtil {
	@Autowired
	private StringRedisTemplate redis;

	private final Hashids hashidsRoom = new Hashids("RoomSalt", 12, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
	private final Hashids hashidsUser = new Hashids("UserSalt", 12, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
	private final Hashids hashidsSessionId = new Hashids("SessionIdSalt", 12, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");

	public String nextRoomCode() {
		long seq = redis.opsForValue().increment("global:room:counter");
		return this.hashidsRoom.encode(seq);
	}

	public long decodeRoomCode(String code) {
		long[] vals = this.hashidsRoom.decode(code);
		return vals.length > 0 ? vals[0] : -1;
	}

	public String nextUserCode() {
		long seq = redis.opsForValue().increment("global:user:counter");
		return this.hashidsUser.encode(seq);
	}

	public long decodeUserCode(String code) {
		long[] vals = this.hashidsRoom.decode(code);
		return vals.length > 0 ? vals[0] : -1;
	}

	public String nextSessionId() {
		long seq = redis.opsForValue().increment("global:chat:session");
		return this.hashidsSessionId.encode(seq);
	}

	public long decodeSessionId(String code) {
		long[] vals = this.hashidsSessionId.decode(code);
		return vals.length > 0 ? vals[0] : -1;
	}
}
