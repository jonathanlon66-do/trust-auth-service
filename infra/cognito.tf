resource "aws_cognito_user_pool" "main" {
  name = "trust-user-pool"

  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

  admin_create_user_config {
    allow_admin_create_user_only = true
  }

  password_policy {
    minimum_length    = 8
    require_uppercase = true
    require_lowercase = true
    require_numbers   = true
    require_symbols   = true
  }

  schema {
    name                = "name"
    attribute_data_type = "String"
    required            = true
    mutable             = true

    string_attribute_constraints {
      min_length = 1
      max_length = 256
    }
  }

  tags = local.tags
}

resource "aws_cognito_user_pool_client" "main" {
  name         = "trust-auth-client"
  user_pool_id = aws_cognito_user_pool.main.id

  generate_secret = true

  explicit_auth_flows = [
    "ALLOW_ADMIN_USER_PASSWORD_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH"
  ]
}

# Terraform escribe los valores reales de Cognito en SSM,
# de donde ECS los inyecta al contenedor. Sin copiar nada a mano.
resource "aws_ssm_parameter" "cognito_user_pool_id" {
  name  = "/trust/cognito/user-pool-id"
  type  = "SecureString"
  value = aws_cognito_user_pool.main.id
}

resource "aws_ssm_parameter" "cognito_client_id" {
  name  = "/trust/cognito/client-id"
  type  = "SecureString"
  value = aws_cognito_user_pool_client.main.id
}

resource "aws_ssm_parameter" "cognito_client_secret" {
  name  = "/trust/cognito/client-secret"
  type  = "SecureString"
  value = aws_cognito_user_pool_client.main.client_secret
}
