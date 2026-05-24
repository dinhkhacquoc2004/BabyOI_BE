# Huong dan test JWT va Refresh Token bang Postman

Tai lieu nay dung de test cac API dang nhap, lay JWT va lam moi JWT cua BabyOi BE.

## 1. Dieu kien truoc khi test

BE dang chay local:

```http
http://localhost:8085
```

Tai khoan can test phai da ton tai trong database va da xac nhan email neu API dang yeu cau email verified.

Neu dang test local va chua can security chat, project hien dang permit public cac route, nhung JWT filter van doc token neu co gui `Authorization: Bearer ...`.

## 2. Login lay JWT va Refresh Token

Method:

```http
POST
```

URL:

```http
http://localhost:8085/api/auth/token
```

Headers:

```http
Content-Type: application/json
```

Body chon `raw` -> `JSON`:

```json
{
  "email": "your_email@gmail.com",
  "password": "YourPassword@123"
}
```

Response thanh cong:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "z5WbqFhU...",
  "tokenType": "Bearer",
  "accessTokenExpiresInMillis": 432000000,
  "refreshTokenExpiresInMillis": 2592000000,
  "userId": 1,
  "userName": "mother",
  "email": "your_email@gmail.com",
  "role": "MOTHER"
}
```

Y nghia:

- `accessToken`: JWT dung de goi cac API can dang nhap.
- `refreshToken`: token dung de xin JWT moi khi JWT cu gan het han hoac da het han.
- `accessTokenExpiresInMillis`: thoi gian song cua JWT. Hien tai la `432000000`, tuong duong 5 ngay.
- `refreshTokenExpiresInMillis`: thoi gian song cua refresh token. Hien tai la `2592000000`, tuong duong 30 ngay.

## 3. Goi API bang JWT

Voi cac API can dang nhap, qua tab `Authorization` trong Postman:

- Type: `Bearer Token`
- Token: dan gia tri `accessToken`

Hoac them header thu cong:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Vi du:

```http
GET http://localhost:8085/api/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

## 4. Refresh JWT

Method:

```http
POST
```

URL:

```http
http://localhost:8085/api/auth/refresh-token
```

Headers:

```http
Content-Type: application/json
```

Body:

```json
{
  "refreshToken": "refreshToken_vua_nhan_duoc_o_buoc_login"
}
```

Response thanh cong:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "refresh_token_moi",
  "tokenType": "Bearer",
  "accessTokenExpiresInMillis": 432000000,
  "refreshTokenExpiresInMillis": 2592000000,
  "userId": 1,
  "userName": "mother",
  "email": "your_email@gmail.com",
  "role": "MOTHER"
}
```

Quan trong:

- Sau khi refresh thanh cong, `refreshToken` cu se bi huy.
- Lan refresh tiep theo phai dung `refreshToken` moi vua duoc tra ve.
- Day la co che sliding session: neu nguoi dung con dung app va con refresh dung han, phien dang nhap se duoc gia han tiep.

## 5. Test nhanh luong hoat dong

1. Goi `POST /api/auth/token`.
2. Copy `accessToken`.
3. Goi mot API bat ky va gan Bearer token.
4. Copy `refreshToken`.
5. Goi `POST /api/auth/refresh-token`.
6. Lay `accessToken` moi de goi API.
7. Lay `refreshToken` moi de refresh lan sau.

## 6. Cac loi thuong gap

### Sai email hoac mat khau

Status co the la `401 Unauthorized`.

Kiem tra lai:

- Email co ton tai trong DB khong.
- Mat khau co dung khong.
- Tai khoan co bi khoa khong.

### Chua xac nhan email

Status co the la `403 Forbidden`.

Tai khoan can xac nhan OTP email truoc khi dang nhap bang password.

### Refresh token khong hop le

Status co the la `401 Unauthorized`.

Nguyen nhan hay gap:

- Gui sai refresh token.
- Refresh token da het han.
- Refresh token da duoc dung roi.
- Da refresh thanh cong mot lan nhung van dung token cu de refresh lan tiep theo.

### JWT het han

Neu `accessToken` het han, goi:

```http
POST /api/auth/refresh-token
```

de lay `accessToken` moi.

## 7. Config hien tai

Trong `application.yaml`:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:VGhpcy1pcy1CYWJ5T0ktZGV2LWp3dC1zZWNyZXQtMzItYnl0ZXMhISE=}
    expiration-millis: 432000000
    refresh-expiration-millis: 2592000000
```

Y nghia:

- `secret`: khoa ky JWT. Local co fallback de de chay.
- `expiration-millis`: han cua access token.
- `refresh-expiration-millis`: han cua refresh token.

Khi deploy production, nen doi thanh:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
```

va set `JWT_SECRET` tren server bang Environment Variable.
