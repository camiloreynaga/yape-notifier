<?php

namespace Tests\Unit;

use App\Models\Commerce;
use App\Models\Device;
use App\Models\DeviceLinkCode;
use App\Models\User;
use App\Services\DeviceLinkService;
use Carbon\Carbon;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class DeviceLinkServiceTest extends TestCase
{
    use RefreshDatabase;

    private DeviceLinkService $service;

    protected function setUp(): void
    {
        parent::setUp();
        $this->service = new DeviceLinkService();
    }

    public function test_it_generates_link_code(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);

        $linkCode = $this->service->generateLinkCode($commerce->id);

        $this->assertInstanceOf(DeviceLinkCode::class, $linkCode);
        $this->assertEquals($commerce->id, $linkCode->commerce_id);
        $this->assertEquals(8, strlen($linkCode->code));
        $this->assertTrue($linkCode->expires_at->isFuture());
        $this->assertNull($linkCode->used_at);
        $this->assertNull($linkCode->device_id);
    }

    public function test_generated_code_expires_in_24_hours(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);

        $linkCode = $this->service->generateLinkCode($commerce->id);

        $expectedExpiry = now()->addHours(24);
        $this->assertTrue(
            $linkCode->expires_at->diffInMinutes($expectedExpiry) < 1,
            'Code should expire approximately 24 hours from now'
        );
    }

    public function test_it_validates_valid_code(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $linkCode = DeviceLinkCode::factory()->create([
            'commerce_id' => $commerce->id,
            'expires_at' => now()->addHours(24),
        ]);

        $result = $this->service->validateCode($linkCode->code);

        $this->assertTrue($result['valid']);
        $this->assertEquals($commerce->id, $result['commerce_id']);
        $this->assertEquals('Código válido', $result['message']);
    }

    public function test_it_rejects_invalid_code(): void
    {
        $result = $this->service->validateCode('INVALID1');

        $this->assertFalse($result['valid']);
        $this->assertNull($result['commerce_id']);
        $this->assertEquals('Código no encontrado', $result['message']);
    }

    public function test_it_rejects_expired_code(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $linkCode = DeviceLinkCode::factory()->expired()->create([
            'commerce_id' => $commerce->id,
        ]);

        $result = $this->service->validateCode($linkCode->code);

        $this->assertFalse($result['valid']);
        $this->assertNull($result['commerce_id']);
        $this->assertEquals('Código expirado', $result['message']);
    }

    public function test_it_rejects_used_code(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $device = Device::factory()->create(['user_id' => $user->id]);
        $linkCode = DeviceLinkCode::factory()->used($device)->create([
            'commerce_id' => $commerce->id,
            'expires_at' => now()->addHours(24),
        ]);

        $result = $this->service->validateCode($linkCode->code);

        $this->assertFalse($result['valid']);
        $this->assertNull($result['commerce_id']);
        $this->assertEquals('Código ya utilizado', $result['message']);
    }

    public function test_it_links_device_to_commerce(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $user->update(['commerce_id' => $commerce->id]);
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => null,
        ]);
        $linkCode = DeviceLinkCode::factory()->create([
            'commerce_id' => $commerce->id,
            'expires_at' => now()->addHours(24),
        ]);

        $result = $this->service->linkDevice($linkCode->code, $device->uuid, $user);

        $this->assertTrue($result['success']);
        $this->assertInstanceOf(Device::class, $result['device']);
        $this->assertEquals($commerce->id, $result['device']->commerce_id);
        $this->assertEquals('Dispositivo vinculado exitosamente', $result['message']);

        // Verify code is marked as used
        $linkCode->refresh();
        $this->assertNotNull($linkCode->used_at);
        $this->assertEquals($device->id, $linkCode->device_id);
    }

    public function test_it_fails_to_link_with_invalid_code(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);

        $result = $this->service->linkDevice('INVALID1', $device->uuid, $user);

        $this->assertFalse($result['success']);
        $this->assertNull($result['device']);
        $this->assertEquals('Código no encontrado', $result['message']);
    }

    public function test_it_fails_to_link_with_expired_code(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $device = Device::factory()->create(['user_id' => $user->id]);
        $linkCode = DeviceLinkCode::factory()->expired()->create([
            'commerce_id' => $commerce->id,
        ]);

        $result = $this->service->linkDevice($linkCode->code, $device->uuid, $user);

        $this->assertFalse($result['success']);
        $this->assertNull($result['device']);
        $this->assertEquals('Código expirado', $result['message']);
    }

    public function test_it_fails_to_link_nonexistent_device(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $linkCode = DeviceLinkCode::factory()->create([
            'commerce_id' => $commerce->id,
            'expires_at' => now()->addHours(24),
        ]);

        // Use a valid UUID format that doesn't exist
        $nonExistentUuid = '00000000-0000-0000-0000-000000000000';
        $result = $this->service->linkDevice($linkCode->code, $nonExistentUuid, $user);

        $this->assertFalse($result['success']);
        $this->assertNull($result['device']);
        $this->assertEquals('Dispositivo no encontrado', $result['message']);
    }

    public function test_it_fails_to_link_device_to_different_commerce(): void
    {
        $user = User::factory()->create();
        $commerce1 = Commerce::factory()->create(['owner_user_id' => $user->id]);
        $commerce2 = Commerce::factory()->create();
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'commerce_id' => $commerce1->id,
        ]);
        $linkCode = DeviceLinkCode::factory()->create([
            'commerce_id' => $commerce2->id,
            'expires_at' => now()->addHours(24),
        ]);

        $result = $this->service->linkDevice($linkCode->code, $device->uuid, $user);

        $this->assertFalse($result['success']);
        $this->assertNull($result['device']);
        $this->assertEquals('El dispositivo ya pertenece a otro negocio', $result['message']);
    }

    public function test_it_gets_active_codes_for_commerce(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);

        // Create active codes
        DeviceLinkCode::factory()->count(3)->create([
            'commerce_id' => $commerce->id,
            'expires_at' => now()->addHours(24),
        ]);

        // Create expired code (should not be included)
        DeviceLinkCode::factory()->expired()->create([
            'commerce_id' => $commerce->id,
        ]);

        // Create used code (should not be included)
        $device = Device::factory()->create();
        DeviceLinkCode::factory()->used($device)->create([
            'commerce_id' => $commerce->id,
            'expires_at' => now()->addHours(24),
        ]);

        $codes = $this->service->getActiveCodes($commerce->id);

        $this->assertCount(3, $codes);
        foreach ($codes as $code) {
            $this->assertTrue($code->isValid());
        }
    }

    public function test_it_cleans_up_expired_codes(): void
    {
        $user = User::factory()->create();
        $commerce = Commerce::factory()->create(['owner_user_id' => $user->id]);

        // Create expired unused codes
        DeviceLinkCode::factory()->expired()->count(5)->create([
            'commerce_id' => $commerce->id,
        ]);

        // Create expired used code (should not be deleted)
        $device = Device::factory()->create();
        DeviceLinkCode::factory()->expired()->used($device)->create([
            'commerce_id' => $commerce->id,
        ]);

        // Create valid code (should not be deleted)
        DeviceLinkCode::factory()->create([
            'commerce_id' => $commerce->id,
            'expires_at' => now()->addHours(24),
        ]);

        $deletedCount = $this->service->cleanupExpiredCodes();

        $this->assertEquals(5, $deletedCount);
        $this->assertEquals(2, DeviceLinkCode::count()); // 1 used expired + 1 valid
    }
}

