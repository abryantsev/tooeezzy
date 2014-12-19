db.infomessage.drop();

db.infomessage.save({
    "_id":"payment_success_redirect",
    "m":{
        "en": "Payment accepted",
        "ru": "Оплата принята"
    },
    "c":0
});

db.infomessage.save({
    "_id":"payment_error_redirect",
    "m":{
        "en": "Payment failed",
        "ru": "Отплата отклонена"
    },
    "c":141
});

db.infomessage.save({
    "_id":"payment_wrong_price",
    "m":{
        "en": "Product price is incorrect. Perhaps it's been changed.",
        "ru": "Некорректная цена товара. Возможно она была изменена, попробуйте еще раз."
    },
    "c":142
});

db.infomessage.save({
    "_id":"payment_present_already_given",
    "m":{
        "en": "Present has been already given. It's not allowed to take the same free present more than once.",
        "ru": "Подарок уже подарен. Нельзя получить один и тот же бесплатный подарок более одного раза."
    },
    "c":143
});

db.infomessage.save({
    "_id":"invalid_main_avatar",
    "m":{
        "en": "This avatar is not in list of your media",
        "ru": "В списке ваших автаров нет этого изображения" //edit
    },
    "c":141
});

db.infomessage.save({
    "_id":"user_not_authenticated",
    "m":{
        "en": "User not authenticated",
        "ru": "Пользователь не аутентифицирован"
    },
    "c":142
});

db.infomessage.save({
    "_id":"PaymentStatus(ok)",
    "m":{
        "en": "Payment has succeeded",
        "ru": "Оплата завершена успешно"
    },
    "c":0
});

db.infomessage.save({
    "_id":"PaymentStatus(process)",
    "m":{
        "en": "Payment is in process",
        "ru": "Производится оплата"
    },
    "c":0
});

db.infomessage.save({
    "_id":"PaymentStatus(failure)",
    "m":{
        "en": "Payment has failed",
        "ru": "Оплата не удалась"
    },
    "c":0
});

db.infomessage.save({
  "_id":"photoalbum_default_name",
  "m":{
    "en": "Mobile upload",
    "ru": "Загруженное с телефона"
  },
  "c":0
});

db.infomessage.save({
  "_id":"RegistrationStatus(ok)",
  "m":{
    "en": "Successful registration",
    "ru": "Регистрация успешно завершена"
  },
  "c":0
});

db.infomessage.save({
  "_id":"photoalbum_new_name",
  "m":{
    "en": "New photoalbum",
    "ru": "Новый фотоальбом"
  },
  "c":0
});

db.infomessage.save({
  "_id":"duplicate_location_own_category_id",
  "m":{
    "en": "Category with such name already exists in this location",
    "ru": "Категория с таким именем уже существует для данного заведения"
  },
  "c":0
});

db.infomessage.save({
  "_id":"duplicate_functional_phone_number",
  "m":{
    "en": "User with such country code and phone number already exists in system",
    "ru": "Пользователем с таким кодом страны и номером телефона уже существует в системе"
  },
  "c":0
});

db.infomessage.save({
  "_id":"present_is_expired",
  "m":{
    "en": "Present is already expired",
    "ru": "У данного подарка истек срок действия"
  },
  "c":0
});

db.infomessage.save({
  "_id":"present_is_received",
  "m":{
    "en": "Present is already received",
    "ru": "Подарок уже получен"
  },
  "c":0
});
db.infomessage.save({
  "_id":"regular_user_can't_send_friendship_request_to_star",
  "m":{
    "en": "Regular user can't send friendship request to star user",
    "ru": "Обычный пользователь не может добавлять в друзья пользователей звезд"
  },
  "c":0
});

db.infomessage.save({
    "_id":"too_many_requests_in_a_given_amount_of_time",
    "m":{
        "en": "The user has sent too many requests in a given amount of time",
        "ru": "Пользователь отправил слишком много запросов за данный временной промежуток"
    },
    "c":429
});
db.infomessage.save({
    "_id":"wrong_combination_of_statistics_fields",
    "m":{
        "en": "Wrong combination of user's statistics fields",
        "ru": "Недопустимое значение параметра statistics"
    },
    "c":0
});
db.infomessage.save({
    "_id":"overflow_max_user_avatar_count",
    "m":{
        "en": "Cannot set more than 5 avatars",
        "ru": "Нельзя устанавливать более 5 аватаров"
    },
    "c":0
});
db.infomessage.save({
    "_id":"media_with_such_url_already_exists",
    "m":{
        "en": "Media with such URI already exists",
        "ru": "Медиа объект с таким URI уже существует"
    },
    "c":0
});

db.infomessage.save({
    "_id":"media_with_such_url_not_exists",
    "m":{
        "en": "Media with such URI not exists",
        "ru": "Медиа объект с таким URI отсутствует"
    },
    "c":0
});

db.infomessage.save({
    "_id":"reached_the_limit_of_users_avatar_quantity",
    "m":{
        "en": "Reached the limit of users avatar quantity",
        "ru": "Превышен лимит аватарок пользователя"
    },
    "c":0
});

db.infomessage.save({
    "_id":"location_approved_by_moderator",
    "m":{
        "en": "Location \"%s\" is approved by moderator",
        "ru": "Заведение \"%s\" одобрено модератором"
    },
    "c":0
});

db.infomessage.save({
    "_id":"company_changes_approved_by_moderator",
    "m":{
        "en": "Data changes are approved by moderator",
        "ru": "Изменения партнера одобрены модератором"
    },
    "c":0
});


db.infomessage.save({
    "_id":"registration_not_confirmed_f",
    "m":{
        "en": "Dear %s, you have passed registration, but haven't activated your account. The message with detailed instructions was sent to your e-mail",
        "ru": "Уважаемая %s, вы прошли регистрацию, но забыли активировать аккаунт. На ваш адрес электронной почты было отправлено письмо с подробными инструкциями"
    },
    "c":0
});

db.infomessage.save({
    "_id":"registration_not_confirmed_m",
    "m":{
        "en": "Dear %s, you have passed registration, but haven't activated your account. The message with detailed instructions was sent to your e-mail",
        "ru": "Уважаемый %s, вы прошли регистрацию, но забыли активировать аккаунт. На ваш адрес электронной почты было отправлено письмо с подробными инструкциями"
    },
    "c":0
});

db.infomessage.save({
    "_id":"max_number_user_phones_has_been_achieved",
    "m":{
        "en": "Max number of user phones has been achieved",
        "ru": "Достигнуто максимально допустимое количество телефонов пользователя"
    },
    "c":0
});

db.infomessage.save({
    "_id":"non_unique_user_phone_numbers",
    "m":{
        "en": "Next phones are already in user by some other users",
        "ru": "Указанные номера используются другими пользователями"
    },
    "c":0
});

db.infomessage.save({
    "_id":"double_check_in_is_not_allowed",
    "m":{
        "en": "Such a check-in already exists",
        "ru": "Такой check-in уже существует"
    },
    "c":0
});

db.infomessage.save({
    "_id":"update_company_with_waiting_status",
    "m":{
        "en": "Company data is already on moderation and can't be changed",
        "ru": "Данные компании уже находится на модерации и не могут быть изменены"
    },
    "c":0
});

db.infomessage.save({
    "_id":"update_location_with_waiting_status",
    "m":{
        "en": "Location data is already on moderation and can't be changed",
        "ru": "Данные заведения уже находится на модерации и не могут быть изменены"
    },
    "c":0
});