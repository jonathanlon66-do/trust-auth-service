resource "aws_ecs_cluster" "main" {
  name = "trust-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = local.tags
}

resource "aws_cloudwatch_log_group" "auth_service" {
  name              = "/ecs/${local.name_prefix}"
  retention_in_days = 30
  tags              = local.tags
}

resource "aws_ecs_task_definition" "auth_service" {
  family                   = "trust-auth-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name      = "auth-service"
    image     = "${aws_ecr_repository.auth_service.repository_url}:latest"
    essential = true

    portMappings = [{
      containerPort = var.container_port
      protocol      = "tcp"
    }]

    environment = [
      { name = "AWS_REGION", value = var.aws_region },
      { name = "SERVER_PORT", value = tostring(var.container_port) }
    ]

    secrets = [
      { name = "AWS_COGNITO_USER_POOL_ID",   valueFrom = "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/trust/cognito/user-pool-id" },
      { name = "AWS_COGNITO_CLIENT_ID",       valueFrom = "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/trust/cognito/client-id" },
      { name = "AWS_COGNITO_CLIENT_SECRET",   valueFrom = "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/trust/cognito/client-secret" },
      { name = "RESEND_API_KEY",              valueFrom = "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/trust/resend/api-key" },
      { name = "RESEND_FROM_EMAIL",           valueFrom = "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/trust/resend/from-email" },
      { name = "JWT_SECRET",                  valueFrom = "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/trust/jwt/secret" },
      { name = "FRONTEND_URL",                valueFrom = "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/trust/frontend-url" }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.auth_service.name
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "ecs"
      }
    }
  }])

  tags = local.tags
}

resource "aws_ecs_service" "auth_service" {
  name            = "trust-auth-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.auth_service.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [aws_subnet.public_a.id, aws_subnet.public_b.id]
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.auth_service.arn
    container_name   = "auth-service"
    container_port   = var.container_port
  }

  depends_on = [aws_lb_listener.http]

  tags = local.tags
}
